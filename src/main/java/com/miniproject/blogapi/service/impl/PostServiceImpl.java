package com.miniproject.blogapi.service.impl;

import com.miniproject.blogapi.dto.PostRequest;
import com.miniproject.blogapi.dto.PostResponse;
import com.miniproject.blogapi.event.PostCreatedEvent;
import com.miniproject.blogapi.exception.AttachmentUploadException;
import com.miniproject.blogapi.exception.ResourceNotFoundException;
import com.miniproject.blogapi.kafka.PostEventProducer;
import com.miniproject.blogapi.model.Post;
import com.miniproject.blogapi.model.PostStatus;
import com.miniproject.blogapi.repository.PostRepository;
import com.miniproject.blogapi.service.CloudinaryService;
import com.miniproject.blogapi.service.PostService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;
    private final PostEventProducer postEventProducer;

    @Override
    @Transactional
    public PostResponse createPost(PostRequest request) {
        return createPost(request, List.of());
    }

    @Override
    @Transactional
    public PostResponse createPost(PostRequest request, List<MultipartFile> files) {
        List<String> uploadedUrls = new ArrayList<>();
        List<String> uploadErrors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String url = cloudinaryService.uploadFile(file);
                uploadedUrls.add(url);
            } catch (AttachmentUploadException e) {
                uploadErrors.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        Post post = new Post();
        post.setText(request.getText());
        post.setAttachments(uploadedUrls);
        post.setRemarks(request.getRemarks());
        post.setStatus(PostStatus.DRAFT);
        post.setCreatedBy(currentUsername());

        Post saved = postRepository.save(post);
        postEventProducer.publishPostCreated(new PostCreatedEvent(saved.getId(), saved.getText()));
        return toResponse(saved, uploadErrors.isEmpty() ? null : uploadErrors);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts() {
        String username = currentUsername();
        boolean admin = isAdmin();

        return postRepository.findAll().stream()
                .filter(post -> admin
                        || post.getStatus() == PostStatus.PUBLISHED
                        || post.getCreatedBy().equals(username))
                .map(post -> toResponse(post, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = findVisibleOrThrow(id);
        return toResponse(post, null);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = findVisibleOrThrow(id);
        assertOwnerOrAdmin(post);

        if (!isAdmin() && post.getStatus() != PostStatus.DRAFT) {
            throw new IllegalStateException("Only draft posts can be edited by their author");
        }

        post.setText(request.getText());
        post.setAttachments(request.getAttachments());
        post.setRemarks(request.getRemarks());
        return toResponse(post, null);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post post = findVisibleOrThrow(id);
        assertOwnerOrAdmin(post);

        if (!isAdmin() && post.getStatus() != PostStatus.DRAFT) {
            throw new IllegalStateException("Only draft posts can be deleted by their author");
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional
    public PostResponse publishPost(Long id) {
        Post post = findPostOrThrow(id);
        post.setStatus(PostStatus.PUBLISHED);
        return toResponse(post, null);
    }

    @Override
    @Transactional
    public PostResponse rejectPost(Long id, String remarks) {
        Post post = findPostOrThrow(id);
        post.setStatus(PostStatus.DRAFT);
        post.setRemarks(remarks);
        return toResponse(post, null);
    }

    private Post findPostOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    private Post findVisibleOrThrow(Long id) {
        Post post = findPostOrThrow(id);
        String username = currentUsername();

        boolean visible = isAdmin()
                || post.getStatus() == PostStatus.PUBLISHED
                || post.getCreatedBy().equals(username);

        if (!visible) {
            throw new ResourceNotFoundException("Post not found with id: " + id);
        }
        return post;
    }

    private void assertOwnerOrAdmin(Post post) {
        if (isAdmin()) {
            return;
        }
        if (!post.getCreatedBy().equals(currentUsername())) {
            throw new ResourceNotFoundException("Post not found with id: " + post.getId());
        }
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    private PostResponse toResponse(Post post, List<String> uploadErrors) {
        return PostResponse.builder()
                .id(post.getId())
                .text(post.getText())
                .attachments(post.getAttachments())
                .status(post.getStatus())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .createdBy(post.getCreatedBy())
                .remarks(post.getRemarks())
                .attachmentUploadErrors(uploadErrors)
                .build();
    }
}
