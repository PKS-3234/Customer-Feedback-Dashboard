package com.customerfeedback.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(AiEmbeddingClient.class);

    private final RestClient restClient;

    public AiEmbeddingClient(@Value("${app.ai-service.base-url:http://localhost:8000}") String aiServiceBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public record EmbeddingResult(String model, int dimension, String vectorString) {}

    public EmbeddingResult requestEmbedding(String text) {
        log.info("Requesting document embedding from AI service (length: {})", text.length());
        Map<String, Object> payload = Map.of(
                "text", text,
                "dimension", 768
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/internal/v1/embeddings/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("AI service returned empty embedding response.");
        }

        String model = response.get("model") != null ? response.get("model").toString() : "gemini-embedding-001";
        int dimension = response.get("dimension") instanceof Number n ? n.intValue() : 768;

        @SuppressWarnings("unchecked")
        List<Number> embeddingValues = (List<Number>) response.get("embedding");
        if (embeddingValues == null || embeddingValues.isEmpty()) {
            throw new IllegalStateException("AI service returned empty vector array.");
        }

        String vectorString = formatAsPgVector(embeddingValues);
        return new EmbeddingResult(model, dimension, vectorString);
    }

    public String requestQueryEmbedding(String query) {
        log.info("Requesting query embedding from AI service for query: '{}'", query);
        Map<String, Object> payload = Map.of(
                "query", query,
                "dimension", 768
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/internal/v1/embeddings/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("AI service returned empty query embedding response.");
        }

        @SuppressWarnings("unchecked")
        List<Number> embeddingValues = (List<Number>) response.get("embedding");
        if (embeddingValues == null || embeddingValues.isEmpty()) {
            throw new IllegalStateException("AI service returned empty vector array for query.");
        }

        return formatAsPgVector(embeddingValues);
    }

    private String formatAsPgVector(List<Number> numbers) {
        return numbers.stream()
                .map(n -> String.format(java.util.Locale.US, "%.8f", n.doubleValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
