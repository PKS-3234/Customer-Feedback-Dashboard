package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final DataSource dataSource;
    private final RestClient restClient;
    private final String aiServiceBaseUrl;

    public HealthController(
            DataSource dataSource,
            @Value("${app.ai-service.base-url:http://localhost:8000}") String aiServiceBaseUrl
    ) {
        this.dataSource = dataSource;
        this.aiServiceBaseUrl = aiServiceBaseUrl;
        this.restClient = RestClient.builder().baseUrl(aiServiceBaseUrl).build();
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        Map<String, Object> details = new HashMap<>();

        // Check database connectivity
        boolean dbHealthy = false;
        try (Connection connection = dataSource.getConnection()) {
            dbHealthy = connection.isValid(2);
            details.put("database", Map.of(
                    "status", dbHealthy ? "UP" : "DOWN",
                    "databaseProductName", connection.getMetaData().getDatabaseProductName()
            ));
        } catch (Exception e) {
            details.put("database", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }

        // Check AI service reachability (non-blocking / optional in Phase 1)
        try {
            var aiResponse = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(Map.class);
            details.put("aiService", Map.of(
                    "status", "UP",
                    "url", aiServiceBaseUrl,
                    "response", aiResponse != null ? aiResponse : Map.of()
            ));
        } catch (Exception e) {
            details.put("aiService", Map.of(
                    "status", "DOWN_OR_UNREACHABLE",
                    "url", aiServiceBaseUrl,
                    "message", "AI service is not currently reachable or running"
            ));
        }

        String overallStatus = dbHealthy ? "UP" : "DEGRADED";

        HealthResponse response = new HealthResponse(
                overallStatus,
                "customer-feedback-platform-backend",
                "1.0.0",
                Instant.now(),
                details
        );

        return ResponseEntity.ok(response);
    }
}
