package com.customerfeedback.platform.dto;

import java.util.List;
import java.util.UUID;

public record BatchAnalysisSummaryResponse(
        UUID batchId,
        int totalRequested,
        int analyzedCount,
        int skippedCount,
        int failedCount,
        List<FeedbackAnalysisResponse> results
) {}
