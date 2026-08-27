package com.miniproject.blogapi.dto;

import com.miniproject.blogapi.model.ModerationStatus;
import com.miniproject.blogapi.model.PostStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String text;
    private List<String> attachments;
    private PostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String remarks;
    private ModerationStatus moderationStatus;
    private String moderationRemarks;
    private List<String> attachmentUploadErrors;
}
