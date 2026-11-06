package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.FeedbackResponse;
import com.customerfeedback.platform.dto.PagedResponse;
import com.customerfeedback.platform.service.FeedbackQueryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final FeedbackQueryService queryService;

    public FeedbackController(FeedbackQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * View stored feedback.
     * Support: batchId, page, size
     */
    @GetMapping
    public ResponseEntity<PagedResponse<FeedbackResponse>> getFeedback(
            @RequestParam(value = "batchId", required = false) UUID batchId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ResponseEntity.ok(queryService.listFeedback(batchId, pageable));
    }
}
