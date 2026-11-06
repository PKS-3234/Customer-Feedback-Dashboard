package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.BatchEmbeddingSummaryResponse;
import com.customerfeedback.platform.dto.FeedbackEmbeddingResponse;
import com.customerfeedback.platform.service.EmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/embeddings")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/feedback/{feedbackId}")
    public ResponseEntity<FeedbackEmbeddingResponse> generateFeedbackEmbedding(
            @PathVariable UUID feedbackId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        FeedbackEmbeddingResponse response = embeddingService.generateFeedbackEmbedding(feedbackId, force);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batches/{batchId}")
    public ResponseEntity<BatchEmbeddingSummaryResponse> generateBatchEmbeddings(
            @PathVariable UUID batchId,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        BatchEmbeddingSummaryResponse summary = embeddingService.generateBatchEmbeddings(batchId, limit, force);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/feedback/{feedbackId}")
    public ResponseEntity<FeedbackEmbeddingResponse> getFeedbackEmbedding(@PathVariable UUID feedbackId) {
        FeedbackEmbeddingResponse response = embeddingService.getFeedbackEmbedding(feedbackId);
        return ResponseEntity.ok(response);
    }
}
