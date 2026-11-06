package com.customerfeedback.platform.dto;

import java.util.List;
import java.util.UUID;

public record BatchEmbeddingSummaryResponse(
        UUID batchId,
        int totalRequested,
        int generatedCount,
        int skippedCount,
        int failedCount,
        List<FeedbackEmbeddingResponse> results
) {
}
