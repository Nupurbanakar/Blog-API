package com.miniproject.blogapi.dto;

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

    // Populated only when one or more attachments failed to upload.
    // Empty/null in the normal case -- clients should check for its
    // presence rather than assuming it's always there.
    private List<String> attachmentUploadErrors;
}
