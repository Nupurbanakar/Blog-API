package com.miniproject.blogapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectRequest {

    @NotBlank(message = "Remarks are required when rejecting a post")
    private String remarks;
}
