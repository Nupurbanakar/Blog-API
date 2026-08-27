package com.miniproject.blogapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
public class ModerationService {

    private static final double TOXICITY_THRESHOLD = 0.5;

    private final WebClient webClient;

    public ModerationService(@Value("${app.huggingface.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://router.huggingface.co/hf-inference/models/unitary/toxic-bert")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public record ModerationResult(boolean flagged, String flaggedCategory) {}

    public ModerationResult moderate(String text) {
        Map<String, Object> requestBody = Map.of("inputs", text);

        try {
            String responseBody = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            return parseResponse(responseBody);
        } catch (WebClientResponseException e) {
            log.error("Hugging Face moderation call failed. Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private ModerationResult parseResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            if (root.has("error")) {
                throw new RuntimeException("Hugging Face model unavailable: " + root.get("error").asText());
            }

            JsonNode scores = root.get(0);
            Iterator<JsonNode> elements = scores.elements();

            while (elements.hasNext()) {
                JsonNode categoryScore = elements.next();
                String label = categoryScore.get("label").asText();
                double score = categoryScore.get("score").asDouble();

                if (score >= TOXICITY_THRESHOLD) {
                    return new ModerationResult(true, label);
                }
            }
            return new ModerationResult(false, null);

        } catch (Exception e) {
            log.error("Failed to parse Hugging Face moderation response: {}", responseBody, e);
            throw new RuntimeException("Failed to parse moderation response", e);
        }
    }
}
