package com.miniproject.blogapi.controller;

import com.miniproject.blogapi.dto.PostRequest;
import com.miniproject.blogapi.dto.PostResponse;
import com.miniproject.blogapi.dto.RejectRequest;
import com.miniproject.blogapi.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Validated
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @RequestParam("text") @NotBlank(message = "Text must not be blank") String text,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        PostRequest request = new PostRequest();
        request.setText(text);
        request.setRemarks(remarks);

        PostResponse created = postService.createPost(request, files == null ? List.of() : files);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return ResponseEntity.ok(postService.updatePost(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> publishPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.publishPost(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> rejectPost(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        return ResponseEntity.ok(postService.rejectPost(id, request.getRemarks()));
    }
}
