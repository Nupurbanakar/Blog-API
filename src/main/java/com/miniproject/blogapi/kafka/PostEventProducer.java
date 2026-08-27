package com.miniproject.blogapi.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniproject.blogapi.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventProducer {

    private static final String TOPIC = "post-created-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishPostCreated(PostCreatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.getPostId().toString(), json);
            log.info("Published PostCreatedEvent for postId={}", event.getPostId());
        } catch (Exception e) {
            log.error("Failed to publish PostCreatedEvent for postId={}", event.getPostId(), e);
        }
    }
}
