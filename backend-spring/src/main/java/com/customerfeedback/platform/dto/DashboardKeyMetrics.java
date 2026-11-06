package com.customerfeedback.platform.dto;

public record DashboardKeyMetrics(
        long totalFeedbackRecords,
        long totalAnalyzedRecords,
        long totalEmbeddedRecords,
        double positiveFeedbackPercent,
        double negativeFeedbackPercent,
        long highPriorityFeedbackCount,
        long currentBatches,
        long processingErrors,
        long failedRows,
        long duplicateRows
) {}
