package com.customerfeedback.platform.dto;

import com.customerfeedback.platform.model.enums.ProcessingStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FeedbackResponse(
        UUID feedbackId,
        UUID batchId,
        LocalDate feedbackDate,
        String channel,
        String product,
        Integer rating,
        String customerSegment,
        String region,
        String feedbackText,
        int originalRowNumber,
        ProcessingStatus processingStatus,
        Instant createdAt
) {}
