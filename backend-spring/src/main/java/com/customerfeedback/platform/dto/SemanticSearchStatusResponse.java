package com.customerfeedback.platform.dto;

public record SemanticSearchStatusResponse(
        String embeddingModel,
        int embeddingDimension,
        long totalEmbeddings,
        String semanticSearchStatus,
        String pgvectorStatus
) {}
