package com.customerfeedback.platform.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public response containing embedding metadata only.
 * Raw high-dimensional vectors are deliberately excluded to keep payloads lightweight and secure.
 */
public record FeedbackEmbeddingResponse(
        UUID embeddingId,
        UUID feedbackId,
        UUID analysisId,
        String embeddingModel,
        Integer embeddingDimension,
        Boolean isCurrent,
        Instant createdAt
) {
}
