package com.miniproject.blogapi.service.impl;

import com.miniproject.blogapi.dto.PostRequest;
import com.miniproject.blogapi.dto.PostResponse;
import com.miniproject.blogapi.exception.ResourceNotFoundException;
import com.miniproject.blogapi.model.Post;
import com.miniproject.blogapi.model.PostStatus;
import com.miniproject.blogapi.repository.PostRepository;
import com.miniproject.blogapi.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    @Override
    @Transactional
    public PostResponse createPost(PostRequest request) {
        Post post = new Post();
        post.setText(request.getText());
        post.setAttachments(request.getAttachments());
        post.setRemarks(request.getRemarks());
        post.setStatus(PostStatus.DRAFT);
        post.setCreatedBy(currentUsername());

        Post saved = postRepository.save(post);
        return toResponse(saved);
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
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = findVisibleOrThrow(id);
        return toResponse(post);
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
        return toResponse(post);
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
        return toResponse(post);
    }

    @Override
    @Transactional
    public PostResponse rejectPost(Long id, String remarks) {
        Post post = findPostOrThrow(id);
        post.setStatus(PostStatus.DRAFT);
        post.setRemarks(remarks);
        return toResponse(post);
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
        String adminAuthority = "ROLE_" + Role.ADMIN.name();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(adminAuthority));
    }

    private PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .text(post.getText())
                .attachments(post.getAttachments())
                .status(post.getStatus())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .createdBy(post.getCreatedBy())
                .remarks(post.getRemarks())
                .build();
    }
}
