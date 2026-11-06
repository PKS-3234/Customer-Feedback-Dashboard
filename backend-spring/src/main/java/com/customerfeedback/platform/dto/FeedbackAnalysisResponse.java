package com.customerfeedback.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeedbackAnalysisResponse(
        UUID analysisId,
        UUID feedbackId,
        String sentiment,
        String category,
        String theme,
        String emotion,
        String priority,
        String department,
        String shortSummary,
        BigDecimal confidenceScore,
        String modelName,
        String modelVersion,
        boolean isCurrent,
        Instant createdAt
) {}
