package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.*;
import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.entity.IngestionBatch;
import com.customerfeedback.platform.entity.ProcessingError;
import com.customerfeedback.platform.exception.FileValidationException;
import com.customerfeedback.platform.exception.ResourceNotFoundException;
import com.customerfeedback.platform.model.enums.BatchStatus;
import com.customerfeedback.platform.model.enums.ProcessingStatus;
import com.customerfeedback.platform.repository.FeedbackRepository;
import com.customerfeedback.platform.repository.IngestionBatchRepository;
import com.customerfeedback.platform.repository.ProcessingErrorRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class BatchIngestionService {

    private static final Logger log = LoggerFactory.getLogger(BatchIngestionService.class);

    private final IngestionBatchRepository batchRepository;
    private final FeedbackRepository feedbackRepository;
    private final ProcessingErrorRepository errorRepository;
    private final CsvValidationService validationService;
    private final DeduplicationService deduplicationService;

    public BatchIngestionService(IngestionBatchRepository batchRepository,
                                 FeedbackRepository feedbackRepository,
                                 ProcessingErrorRepository errorRepository,
                                 CsvValidationService validationService,
                                 DeduplicationService deduplicationService) {
        this.batchRepository = batchRepository;
        this.feedbackRepository = feedbackRepository;
        this.errorRepository = errorRepository;
        this.validationService = validationService;
        this.deduplicationService = deduplicationService;
    }

    @Transactional
    public BatchDetailResponse ingestCsv(MultipartFile file, String sourceSystem, String uploadUser) {
        validationService.validateFileStructure(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new FileValidationException("Failed to read uploaded file: " + e.getMessage());
        }

        String fileHash = deduplicationService.computeFileHash(fileBytes);
        String fileName = file.getOriginalFilename();

        IngestionBatch batch = new IngestionBatch(
                fileName,
                sourceSystem != null && !sourceSystem.isBlank() ? sourceSystem : "WEB_UPLOAD",
                uploadUser != null && !uploadUser.isBlank() ? uploadUser : "system",
                fileHash
        );
        batch.setStatus(BatchStatus.PROCESSING);
        batch = batchRepository.save(batch);

        log.info("Initialized batch ingestion [ID: {}, File: {}, Hash: {}]", batch.getBatchId(), fileName, fileHash);

        int totalRows = 0;
        int successfulRows = 0;
        int failedRows = 0;
        int duplicateRows = 0;

        Set<UUID> seenFeedbackIds = new HashSet<>();
        Set<String> seenFeedbackTextsInBatch = new HashSet<>();

        List<Feedback> feedbackToSave = new ArrayList<>();
        List<ProcessingError> errorsToSave = new ArrayList<>();

        try (InputStream is = new ByteArrayInputStream(fileBytes);
             Reader reader = validationService.createBomAwareReader(is)) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(reader);
            List<String> rawHeaders = parser.getHeaderNames();

            if (rawHeaders == null || rawHeaders.isEmpty()) {
                throw new FileValidationException("No header row detected in CSV file.");
            }

            Map<String, String> headerMap = new HashMap<>();
            for (String header : rawHeaders) {
                String cleanHeader = validationService.stripBom(header).trim();
                headerMap.put(cleanHeader.toLowerCase(), cleanHeader);
            }

            // Check required header presence
            for (String required : CsvValidationService.REQUIRED_COLUMNS) {
                if (!headerMap.containsKey(required.toLowerCase())) {
                    throw new FileValidationException("Missing required CSV header: " + required);
                }
            }

            for (CSVRecord record : parser) {
                if (validationService.isRecordCompletelyBlank(record)) {
                    continue;
                }

                totalRows++;
                int rowNumber = (int) record.getRecordNumber();
                List<RowValidationErrorDto> validationErrors = validationService.validateRecord(record, headerMap, rowNumber);

                if (!validationErrors.isEmpty()) {
                    failedRows++;
                    for (RowValidationErrorDto err : validationErrors) {
                        errorsToSave.add(new ProcessingError(
                                batch,
                                rowNumber,
                                record.toString(),
                                err.field() + "_INVALID",
                                err.message()
                        ));
                    }
                    continue;
                }

                // Parse fields
                String feedbackIdStr = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_FEEDBACK_ID);
                UUID feedbackId;
                if (feedbackIdStr != null && !feedbackIdStr.isBlank()) {
                    feedbackId = UUID.fromString(feedbackIdStr.trim());
                } else {
                    feedbackId = UUID.randomUUID();
                }

                String feedbackText = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_FEEDBACK_TEXT);
                String normalizedText = deduplicationService.normalizeFeedbackText(feedbackText);

                // 1. Check duplicate by feedback_id
                boolean duplicateId = false;
                if (feedbackIdStr != null && !feedbackIdStr.isBlank()) {
                    if (seenFeedbackIds.contains(feedbackId) || feedbackRepository.existsByFeedbackId(feedbackId)) {
                        duplicateId = true;
                    }
                }

                if (duplicateId) {
                    duplicateRows++;
                    errorsToSave.add(new ProcessingError(
                            batch,
                            rowNumber,
                            record.toString(),
                            "DUPLICATE_FEEDBACK_ID",
                            "Duplicate feedback_id detected: " + feedbackId
                    ));
                    continue;
                }

                // 2. Check duplicate by same feedback_text within the same batch
                boolean duplicateText = seenFeedbackTextsInBatch.contains(normalizedText);

                Feedback feedback = new Feedback();
                feedback.setFeedbackId(feedbackId);
                feedback.setBatch(batch);
                feedback.setFeedbackDate(validationService.parseDate(validationService.getFieldValue(record, headerMap, CsvValidationService.COL_FEEDBACK_DATE)));
                feedback.setChannel(validationService.getFieldValue(record, headerMap, CsvValidationService.COL_CHANNEL));
                feedback.setProduct(validationService.getFieldValue(record, headerMap, CsvValidationService.COL_PRODUCT));
                feedback.setRating(validationService.parseRating(validationService.getFieldValue(record, headerMap, CsvValidationService.COL_RATING)));
                feedback.setCustomerSegment(validationService.getFieldValue(record, headerMap, CsvValidationService.COL_CUSTOMER_SEGMENT));
                feedback.setRegion(validationService.getFieldValue(record, headerMap, CsvValidationService.COL_REGION));
                feedback.setFeedbackText(feedbackText);
                feedback.setOriginalRowNumber(rowNumber);

                if (duplicateText) {
                    duplicateRows++;
                    feedback.setProcessingStatus(ProcessingStatus.DUPLICATE);
                    errorsToSave.add(new ProcessingError(
                            batch,
                            rowNumber,
                            record.toString(),
                            "DUPLICATE_FEEDBACK_TEXT",
                            "Duplicate feedback text detected within batch: '" + (feedbackText.length() > 50 ? feedbackText.substring(0, 50) + "..." : feedbackText) + "'"
                    ));
                    feedbackToSave.add(feedback);
                } else {
                    successfulRows++;
                    feedback.setProcessingStatus(ProcessingStatus.STORED);
                    feedbackToSave.add(feedback);
                }

                seenFeedbackIds.add(feedbackId);
                seenFeedbackTextsInBatch.add(normalizedText);
            }

            // Save feedback and errors in batches
            if (!feedbackToSave.isEmpty()) {
                feedbackRepository.saveAll(feedbackToSave);
            }
            if (!errorsToSave.isEmpty()) {
                errorRepository.saveAll(errorsToSave);
            }

            // Finalize batch metrics
            batch.setTotalRows(totalRows);
            batch.setSuccessfulRows(successfulRows);
            batch.setFailedRows(failedRows);
            batch.setDuplicateRows(duplicateRows);
            batch.setCompletedAt(Instant.now());

            if (failedRows == 0 && duplicateRows == 0 && totalRows > 0) {
                batch.setStatus(BatchStatus.COMPLETED);
            } else if (successfulRows > 0) {
                batch.setStatus(BatchStatus.COMPLETED_WITH_ERRORS);
            } else {
                batch.setStatus(BatchStatus.FAILED);
            }

            batch = batchRepository.save(batch);
            log.info("Completed batch [ID: {}, Total: {}, Success: {}, Failed: {}, Dup: {}, Status: {}]",
                    batch.getBatchId(), totalRows, successfulRows, failedRows, duplicateRows, batch.getStatus());

            return toDetailResponse(batch);

        } catch (FileValidationException e) {
            batch.setStatus(BatchStatus.FAILED);
            batch.setCompletedAt(Instant.now());
            batchRepository.save(batch);
            throw e;
        } catch (Exception e) {
            log.error("Unhandled error ingesting CSV batch", e);
            batch.setStatus(BatchStatus.FAILED);
            batch.setCompletedAt(Instant.now());
            batchRepository.save(batch);
            throw new FileValidationException("Ingestion error: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<BatchSummaryResponse> listBatches(Pageable pageable) {
        Page<IngestionBatch> page = batchRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PagedResponse.from(page.map(this::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public BatchDetailResponse getBatchDetail(UUID batchId) {
        IngestionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with ID: " + batchId));
        return toDetailResponse(batch);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProcessingErrorResponse> listBatchErrors(UUID batchId, Pageable pageable) {
        if (!batchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("Batch not found with ID: " + batchId);
        }
        Page<ProcessingError> page = errorRepository.findByBatch_BatchIdOrderByRowNumberAsc(batchId, pageable);
        return PagedResponse.from(page.map(this::toErrorResponse));
    }

    public BatchSummaryResponse toSummaryResponse(IngestionBatch batch) {
        return new BatchSummaryResponse(
                batch.getBatchId(),
                batch.getSourceFileName(),
                batch.getSourceSystem(),
                batch.getUploadUser(),
                batch.getTotalRows(),
                batch.getSuccessfulRows(),
                batch.getFailedRows(),
                batch.getDuplicateRows(),
                batch.getStatus(),
                batch.getCreatedAt(),
                batch.getCompletedAt()
        );
    }

    public BatchDetailResponse toDetailResponse(IngestionBatch batch) {
        return new BatchDetailResponse(
                batch.getBatchId(),
                batch.getSourceFileName(),
                batch.getSourceSystem(),
                batch.getUploadUser(),
                batch.getFileHash(),
                batch.getTotalRows(),
                batch.getSuccessfulRows(),
                batch.getFailedRows(),
                batch.getDuplicateRows(),
                batch.getStatus(),
                batch.getCreatedAt(),
                batch.getCompletedAt()
        );
    }

    public ProcessingErrorResponse toErrorResponse(ProcessingError err) {
        return new ProcessingErrorResponse(
                err.getErrorId(),
                err.getBatch().getBatchId(),
                err.getRowNumber(),
                err.getRawRecord(),
                err.getErrorType(),
                err.getErrorMessage(),
                err.getCreatedAt()
        );
    }
}
