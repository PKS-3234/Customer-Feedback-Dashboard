package com.customerfeedback.platform.dto;

import com.customerfeedback.platform.model.enums.BatchStatus;
import java.time.Instant;
import java.util.UUID;

public record BatchDetailResponse(
        UUID batchId,
        String sourceFileName,
        String sourceSystem,
        String uploadUser,
        String fileHash,
        int totalRows,
        int successfulRows,
        int failedRows,
        int duplicateRows,
        BatchStatus status,
        Instant createdAt,
        Instant completedAt
) {}
