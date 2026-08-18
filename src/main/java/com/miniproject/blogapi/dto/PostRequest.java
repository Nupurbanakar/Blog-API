package com.miniproject.blogapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PostRequest {
    @NotBlank(message = "Text must not be blank")
    private String text;
    private List<String> attachments;
    private String remarks;
}
