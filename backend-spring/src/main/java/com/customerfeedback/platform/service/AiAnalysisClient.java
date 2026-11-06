package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.FeedbackAnalysisResponse;
import com.customerfeedback.platform.entity.Feedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Service
public class AiAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisClient.class);

    private final RestClient restClient;
    private final String aiServiceBaseUrl;

    public AiAnalysisClient(@Value("${app.ai-service.base-url:http://localhost:8000}") String aiServiceBaseUrl) {
        this.aiServiceBaseUrl = aiServiceBaseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public FeedbackAnalysisResponse requestAnalysis(Feedback feedback) {
        log.info("Requesting Gemini analysis from AI service for feedback ID: {}", feedback.getFeedbackId());

        Map<String, Object> payload = Map.of(
                "feedback_id", feedback.getFeedbackId().toString(),
                "feedback_text", feedback.getFeedbackText(),
                "channel", feedback.getChannel() != null ? feedback.getChannel() : "",
                "product", feedback.getProduct() != null ? feedback.getProduct() : "",
                "rating", feedback.getRating() != null ? feedback.getRating() : 0,
                "customer_segment", feedback.getCustomerSegment() != null ? feedback.getCustomerSegment() : "",
                "region", feedback.getRegion() != null ? feedback.getRegion() : ""
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/internal/v1/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("AI service returned empty response for feedback: " + feedback.getFeedbackId());
            }

            String modelName = response.get("model_name") != null ? response.get("model_name").toString() : "gemini-2.5-flash";
            String modelVersion = response.get("model_version") != null ? response.get("model_version").toString() : "gemini-2.5-flash";

            return new FeedbackAnalysisResponse(
                    null, // analysisId assigned upon entity persistence
                    feedback.getFeedbackId(),
                    (String) response.get("sentiment"),
                    (String) response.get("category"),
                    (String) response.get("theme"),
                    (String) response.get("emotion"),
                    (String) response.get("priority"),
                    (String) response.get("department"),
                    (String) response.get("short_summary"),
                    BigDecimal.valueOf(((Number) response.get("confidence_score")).doubleValue()),
                    modelName,
                    modelVersion,
                    true,
                    null
            );

        } catch (Exception e) {
            log.error("Failed to execute AI analysis for feedback ID: {}. Error: {}", feedback.getFeedbackId(), e.getMessage());
            throw new RuntimeException("AI service analysis failed: " + e.getMessage(), e);
        }
    }
}
