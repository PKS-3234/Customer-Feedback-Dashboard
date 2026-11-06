package com.customerfeedback.platform.dto;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
        String status,
        String service,
        String version,
        Instant timestamp,
        Map<String, Object> details
) {
    public HealthResponse(String status, String service, String version, Instant timestamp) {
        this(status, service, version, timestamp, Map.of());
    }
}
