package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.BatchAnalysisSummaryResponse;
import com.customerfeedback.platform.dto.FeedbackAnalysisResponse;
import com.customerfeedback.platform.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Analyze one feedback record using Gemini.
     */
    @PostMapping("/feedback/{feedbackId}")
    public ResponseEntity<FeedbackAnalysisResponse> analyzeFeedback(
            @PathVariable UUID feedbackId,
            @RequestParam(value = "reanalyze", defaultValue = "false") boolean reanalyze
    ) {
        FeedbackAnalysisResponse response = analysisService.analyzeFeedback(feedbackId, reanalyze);
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze all eligible records in a batch.
     */
    @PostMapping("/batches/{batchId}")
    public ResponseEntity<BatchAnalysisSummaryResponse> analyzeBatch(
            @PathVariable UUID batchId,
            @RequestParam(value = "limit", defaultValue = "100") int limit,
            @RequestParam(value = "reanalyze", defaultValue = "false") boolean reanalyze
    ) {
        BatchAnalysisSummaryResponse response = analysisService.analyzeBatch(batchId, limit, reanalyze);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve current analysis for a feedback record.
     */
    @GetMapping("/feedback/{feedbackId}")
    public ResponseEntity<FeedbackAnalysisResponse> getCurrentAnalysis(
            @PathVariable UUID feedbackId
    ) {
        FeedbackAnalysisResponse response = analysisService.getCurrentAnalysis(feedbackId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve current analyses for all records in a batch.
     */
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<List<FeedbackAnalysisResponse>> getBatchAnalyses(
            @PathVariable UUID batchId
    ) {
        List<FeedbackAnalysisResponse> response = analysisService.getBatchAnalyses(batchId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve full analysis audit history for a feedback record.
     */
    @GetMapping("/feedback/{feedbackId}/history")
    public ResponseEntity<List<FeedbackAnalysisResponse>> getAnalysisHistory(
            @PathVariable UUID feedbackId
    ) {
        List<FeedbackAnalysisResponse> response = analysisService.getFeedbackAnalysisHistory(feedbackId);
        return ResponseEntity.ok(response);
    }
}
