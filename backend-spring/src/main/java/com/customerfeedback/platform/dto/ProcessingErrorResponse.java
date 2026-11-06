package com.customerfeedback.platform.dto;

import java.time.Instant;
import java.util.UUID;

public record ProcessingErrorResponse(
        UUID errorId,
        UUID batchId,
        Integer rowNumber,
        String rawRecord,
        String errorType,
        String errorMessage,
        Instant createdAt
) {}
