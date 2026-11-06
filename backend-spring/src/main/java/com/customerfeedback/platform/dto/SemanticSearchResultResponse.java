package com.customerfeedback.platform.dto;

import java.util.UUID;

public record SemanticSearchResultResponse(
        UUID feedbackId,
        String feedbackText,
        Double similarity,
        String category,
        String theme,
        String sentiment,
        String priority
) {
}
