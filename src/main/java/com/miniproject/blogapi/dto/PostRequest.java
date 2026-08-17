package com.miniproject.blogapi.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PostRequest {
    private String text;
    private List<String> attachments;
    private String remarks;
}
