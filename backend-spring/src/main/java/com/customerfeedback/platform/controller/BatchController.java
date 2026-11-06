package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.*;
import com.customerfeedback.platform.service.BatchIngestionService;
import com.customerfeedback.platform.service.CsvValidationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/batches")
public class BatchController {

    private final CsvValidationService validationService;
    private final BatchIngestionService ingestionService;

    public BatchController(CsvValidationService validationService, BatchIngestionService ingestionService) {
        this.validationService = validationService;
        this.ingestionService = ingestionService;
    }

    /**
     * Validate uploaded file without persisting.
     */
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ValidateCsvResponse> validateCsv(
            @RequestParam("file") MultipartFile file
    ) {
        ValidateCsvResponse response = validationService.previewAndValidate(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload and process CSV.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchDetailResponse> uploadAndProcessBatch(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceSystem", required = false, defaultValue = "WEB_UPLOAD") String sourceSystem,
            @RequestParam(value = "uploadUser", required = false, defaultValue = "system") String uploadUser
    ) {
        BatchDetailResponse response = ingestionService.ingestCsv(file, sourceSystem, uploadUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List batches with pagination.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<BatchSummaryResponse>> listBatches(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ResponseEntity.ok(ingestionService.listBatches(pageable));
    }

    /**
     * Batch details.
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<BatchDetailResponse> getBatchDetail(
            @PathVariable UUID batchId
    ) {
        return ResponseEntity.ok(ingestionService.getBatchDetail(batchId));
    }

    /**
     * View ingestion errors with pagination.
     */
    @GetMapping("/{batchId}/errors")
    public ResponseEntity<PagedResponse<ProcessingErrorResponse>> getBatchErrors(
            @PathVariable UUID batchId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ResponseEntity.ok(ingestionService.listBatchErrors(batchId, pageable));
    }
}
