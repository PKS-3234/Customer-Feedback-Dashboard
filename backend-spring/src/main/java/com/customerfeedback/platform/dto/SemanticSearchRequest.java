package com.customerfeedback.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SemanticSearchRequest(
        @NotBlank(message = "Search query must not be blank")
        String query,

        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 100, message = "Limit cannot exceed 100")
        Integer limit
) {
    public SemanticSearchRequest {
        if (limit == null) {
            limit = 10;
        }
    }
}
