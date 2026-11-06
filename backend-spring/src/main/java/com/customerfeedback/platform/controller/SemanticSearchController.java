package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.SemanticSearchRequest;
import com.customerfeedback.platform.dto.SemanticSearchResultResponse;
import com.customerfeedback.platform.service.EmbeddingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/semantic-search")
public class SemanticSearchController {

    private final EmbeddingService embeddingService;

    public SemanticSearchController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping
    public ResponseEntity<List<SemanticSearchResultResponse>> search(
            @Valid @RequestBody SemanticSearchRequest request) {
        List<SemanticSearchResultResponse> results = embeddingService.performSemanticSearch(
                request.query(),
                request.limit()
        );
        return ResponseEntity.ok(results);
    }
}
