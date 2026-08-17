package com.miniproject.blogapi.service.impl;

import com.miniproject.blogapi.dto.PostRequest;
import com.miniproject.blogapi.dto.PostResponse;
import com.miniproject.blogapi.model.Post;
import com.miniproject.blogapi.model.PostStatus;
import com.miniproject.blogapi.repository.PostRepository;
import com.miniproject.blogapi.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

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
        post.setCreatedBy("temp-user");

        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts() {
        return postRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = findPostOrThrow(id);
        return toResponse(post);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = findPostOrThrow(id);
        post.setText(request.getText());
        post.setAttachments(request.getAttachments());
        post.setRemarks(request.getRemarks());
        return toResponse(post);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post post = findPostOrThrow(id);
        postRepository.delete(post);
    }

    private Post findPostOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found with id: " + id));
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
