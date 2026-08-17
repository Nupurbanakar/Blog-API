package com.miniproject.blogapi.dto;

import com.miniproject.blogapi.model.PostStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class PostResponse {
    private Long id;
    private String text;
    private List<String> attachments;
    private PostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String remarks;
}
