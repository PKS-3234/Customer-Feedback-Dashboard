package com.customerfeedback.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AgentQueryRequest(
        @NotBlank(message = "Query must not be blank")
        String query,

        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 50, message = "Limit cannot exceed 50")
        Integer limit
) {
    public AgentQueryRequest {
        if (limit == null) {
            limit = 5;
        }
    }
}
