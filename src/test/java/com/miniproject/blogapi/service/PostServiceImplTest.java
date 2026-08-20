package com.miniproject.blogapi.service;

import com.miniproject.blogapi.dto.PostRequest;
import com.miniproject.blogapi.dto.PostResponse;
import com.miniproject.blogapi.exception.ResourceNotFoundException;
import com.miniproject.blogapi.model.Post;
import com.miniproject.blogapi.model.PostStatus;
import com.miniproject.blogapi.repository.PostRepository;
import com.miniproject.blogapi.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostServiceImpl postService;

    @BeforeEach
    void setUp() {
        loginAs("user", "ROLE_USER");
    }

    private void loginAs(String username, String... authorities) {
        var auths = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(username, null, auths);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void createPost_setsStatusToDraftAndCreatedByToCurrentUser() {
        PostRequest request = new PostRequest();
        request.setText("Hello world");
        request.setAttachments(List.of());
        request.setRemarks("first draft");

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PostResponse response = postService.createPost(request);

        assertThat(response.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(response.getCreatedBy()).isEqualTo("user");
        assertThat(response.getText()).isEqualTo("Hello world");
    }

    @Test
    void getPostById_throwsNotFound_whenPostDoesNotExist() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getPostById_throwsNotFound_whenDraftBelongsToSomeoneElse() {
        Post othersDraft = new Post();
        othersDraft.setId(5L);
        othersDraft.setStatus(PostStatus.DRAFT);
        othersDraft.setCreatedBy("alice");

        when(postRepository.findById(5L)).thenReturn(Optional.of(othersDraft));

        assertThatThrownBy(() -> postService.getPostById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPostById_succeeds_whenPostIsPublished_evenIfNotOwnedByCaller() {
        Post publishedPost = new Post();
        publishedPost.setId(7L);
        publishedPost.setStatus(PostStatus.PUBLISHED);
        publishedPost.setCreatedBy("alice");
        publishedPost.setText("Published content");

        when(postRepository.findById(7L)).thenReturn(Optional.of(publishedPost));

        PostResponse response = postService.getPostById(7L);

        assertThat(response.getText()).isEqualTo("Published content");
    }

    @Test
    void updatePost_throwsConflict_whenNonAdminEditsPublishedPost() {
        Post published = new Post();
        published.setId(3L);
        published.setStatus(PostStatus.PUBLISHED);
        published.setCreatedBy("user");

        when(postRepository.findById(3L)).thenReturn(Optional.of(published));

        PostRequest request = new PostRequest();
        request.setText("trying to edit after publish");

        assertThatThrownBy(() -> postService.updatePost(3L, request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publishPost_succeeds_whenCalledByAdmin() {
        loginAs("admin", "ROLE_ADMIN");

        Post draft = new Post();
        draft.setId(2L);
        draft.setStatus(PostStatus.DRAFT);
        draft.setCreatedBy("user");

        when(postRepository.findById(2L)).thenReturn(Optional.of(draft));

        PostResponse response = postService.publishPost(2L);

        assertThat(response.getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }
}
