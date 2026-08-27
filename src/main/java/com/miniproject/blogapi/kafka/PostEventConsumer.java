package com.miniproject.blogapi.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniproject.blogapi.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "post-created-topic", groupId = "blog-api-moderation-group")
    public void handlePostCreated(String message) {
        try {
            PostCreatedEvent event = objectMapper.readValue(message, PostCreatedEvent.class);
            log.info("Consumed PostCreatedEvent: postId={}, text='{}'", event.getPostId(), event.getText());
        } catch (Exception e) {
            log.error("Failed to process message: {}", message, e);
        }
    }
}
