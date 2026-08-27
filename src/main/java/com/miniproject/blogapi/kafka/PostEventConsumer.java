package com.miniproject.blogapi.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniproject.blogapi.event.PostCreatedEvent;
import com.miniproject.blogapi.model.ModerationStatus;
import com.miniproject.blogapi.model.Post;
import com.miniproject.blogapi.repository.PostRepository;
import com.miniproject.blogapi.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventConsumer {

    private final ObjectMapper objectMapper;
    private final ModerationService moderationService;
    private final PostRepository postRepository;

    @KafkaListener(topics = "post-created-topic", groupId = "blog-api-moderation-group")
    @Transactional
    public void handlePostCreated(String message) {
        PostCreatedEvent event;
        try {
            event = objectMapper.readValue(message, PostCreatedEvent.class);
        } catch (Exception e) {
            // Can't even read the message -- nothing to recover, log and
            // stop. There's no Post to update because we don't know which
            // one this was even meant to be.
            log.error("Failed to deserialize Kafka message: {}", message, e);
            return;
        }

        Post post = postRepository.findById(event.getPostId()).orElse(null);
        if (post == null) {
            // The post was deleted between being created and being
            // moderated -- rare, but possible. Nothing to update.
            log.warn("Post {} no longer exists, skipping moderation", event.getPostId());
            return;
        }

        try {
            ModerationService.ModerationResult result = moderationService.moderate(event.getText());

            if (result.flagged()) {
                post.setModerationStatus(ModerationStatus.REJECTED);
                post.setModerationRemarks("Flagged for: " + result.flaggedCategory());
            } else {
                post.setModerationStatus(ModerationStatus.APPROVED);
            }
            // No explicit save() needed -- @Transactional + dirty checking,
            // same pattern as updatePost() back in the service layer.
            log.info("Moderation complete for postId={}: {}", event.getPostId(), post.getModerationStatus());

        } catch (Exception e) {
            // The moderation API call itself failed (network issue, OpenAI
            // down, timeout). Deliberate choice: leave moderationStatus as
            // PENDING rather than guessing APPROVED or REJECTED -- an
            // admin can retry or manually review later. We do NOT rethrow
            // here, because an uncaught exception in a @KafkaListener
            // would cause Kafka to redeliver this same message
            // indefinitely, retrying against a service that might still
            // be down -- better to log it clearly and move on.
            log.error("Moderation API call failed for postId={}, leaving status PENDING", event.getPostId(), e);
        }
    }
}
