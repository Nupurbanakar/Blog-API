package com.miniproject.blogapi.service;

import com.miniproject.blogapi.dto.PostRequest;
import com.miniproject.blogapi.dto.PostResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface PostService {
    PostResponse createPost(PostRequest request);
    PostResponse createPost(PostRequest request, List<MultipartFile> files);
    List<PostResponse> getAllPosts();
    PostResponse getPostById(Long id);
    PostResponse updatePost(Long id, PostRequest request);
    void deletePost(Long id);
    PostResponse publishPost(Long id);
    PostResponse rejectPost(Long id, String remarks);
}
