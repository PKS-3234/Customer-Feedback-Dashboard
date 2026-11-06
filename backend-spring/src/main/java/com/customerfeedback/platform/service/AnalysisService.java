package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.BatchAnalysisSummaryResponse;
import com.customerfeedback.platform.dto.FeedbackAnalysisResponse;
import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.entity.FeedbackAnalysis;
import com.customerfeedback.platform.exception.ResourceNotFoundException;
import com.customerfeedback.platform.model.enums.ProcessingStatus;
import com.customerfeedback.platform.repository.FeedbackAnalysisRepository;
import com.customerfeedback.platform.repository.FeedbackRepository;
import com.customerfeedback.platform.repository.IngestionBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final FeedbackRepository feedbackRepository;
    private final FeedbackAnalysisRepository analysisRepository;
    private final IngestionBatchRepository batchRepository;
    private final AiAnalysisClient aiAnalysisClient;
    private final EmbeddingService embeddingService;

    public AnalysisService(FeedbackRepository feedbackRepository,
                           FeedbackAnalysisRepository analysisRepository,
                           IngestionBatchRepository batchRepository,
                           AiAnalysisClient aiAnalysisClient,
                           EmbeddingService embeddingService) {
        this.feedbackRepository = feedbackRepository;
        this.analysisRepository = analysisRepository;
        this.batchRepository = batchRepository;
        this.aiAnalysisClient = aiAnalysisClient;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public FeedbackAnalysisResponse analyzeFeedback(UUID feedbackId, boolean reanalyze) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback record not found: " + feedbackId));

        if (feedback.getProcessingStatus() == ProcessingStatus.FAILED) {
            throw new IllegalArgumentException("Cannot analyze failed feedback record: " + feedbackId);
        }

        // Check if current active analysis exists
        var currentOpt = analysisRepository.findByFeedback_FeedbackIdAndIsCurrentTrue(feedbackId);
        if (currentOpt.isPresent() && !reanalyze) {
            log.info("Analysis already exists for feedback {} and reanalyze=false. Skipping Gemini call.", feedbackId);
            return toResponse(currentOpt.get());
        }

        if (reanalyze && currentOpt.isPresent()) {
            log.info("Re-analysis requested for feedback {}. Marking prior analyses as historical.", feedbackId);
            analysisRepository.markPriorAnalysesNotCurrent(feedbackId);
        }

        FeedbackAnalysisResponse aiResult = aiAnalysisClient.requestAnalysis(feedback);

        FeedbackAnalysis entity = new FeedbackAnalysis();
        entity.setFeedback(feedback);
        entity.setSentiment(aiResult.sentiment());
        entity.setCategory(aiResult.category());
        entity.setTheme(aiResult.theme());
        entity.setEmotion(aiResult.emotion());
        entity.setPriority(aiResult.priority());
        entity.setDepartment(aiResult.department());
        entity.setShortSummary(aiResult.shortSummary());
        entity.setConfidenceScore(aiResult.confidenceScore());
        entity.setModelName(aiResult.modelName());
        entity.setModelVersion(aiResult.modelVersion());
        entity.setCurrent(true);

        entity = analysisRepository.save(entity);
        log.info("Successfully persisted analysis {} for feedback {}", entity.getAnalysisId(), feedbackId);

        // Analysis → Embedding Generation → Persistence
        try {
            embeddingService.generateFeedbackEmbedding(feedbackId, false);
            log.info("Successfully generated and persisted embedding for feedback {}", feedbackId);
        } catch (Exception ex) {
            log.warn("Automatic embedding generation deferred for feedback {}: {}", feedbackId, ex.getMessage());
        }

        return toResponse(entity);
    }

    @Transactional
    public BatchAnalysisSummaryResponse analyzeBatch(UUID batchId, int limit, boolean reanalyze) {
        if (!batchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("Ingestion batch not found: " + batchId);
        }

        int maxProcess = Math.min(100, Math.max(1, limit));
        Page<Feedback> page = feedbackRepository.findByBatch_BatchIdOrderByOriginalRowNumberAsc(batchId, PageRequest.of(0, 500));
        List<Feedback> allRecords = page.getContent();

        List<FeedbackAnalysisResponse> results = new ArrayList<>();
        int analyzedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Feedback f : allRecords) {
            if (analyzedCount >= maxProcess) {
                break;
            }

            if (f.getProcessingStatus() == ProcessingStatus.FAILED) {
                skippedCount++;
                continue;
            }

            boolean hasCurrent = analysisRepository.existsByFeedback_FeedbackIdAndIsCurrentTrue(f.getFeedbackId());
            if (hasCurrent && !reanalyze) {
                skippedCount++;
                continue;
            }

            try {
                FeedbackAnalysisResponse res = analyzeFeedback(f.getFeedbackId(), reanalyze);
                results.add(res);
                analyzedCount++;
            } catch (Exception ex) {
                log.error("Failed to analyze feedback record {}: {}", f.getFeedbackId(), ex.getMessage());
                failedCount++;
            }
        }

        return new BatchAnalysisSummaryResponse(
                batchId,
                maxProcess,
                analyzedCount,
                skippedCount,
                failedCount,
                results
        );
    }

    @Transactional(readOnly = true)
    public FeedbackAnalysisResponse getCurrentAnalysis(UUID feedbackId) {
        return analysisRepository.findByFeedback_FeedbackIdAndIsCurrentTrue(feedbackId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No current analysis found for feedback: " + feedbackId));
    }

    @Transactional(readOnly = true)
    public List<FeedbackAnalysisResponse> getBatchAnalyses(UUID batchId) {
        if (!batchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("Ingestion batch not found: " + batchId);
        }
        return analysisRepository.findByFeedback_Batch_BatchIdAndIsCurrentTrueOrderByCreatedAtDesc(batchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackAnalysisResponse> getFeedbackAnalysisHistory(UUID feedbackId) {
        return analysisRepository.findByFeedback_FeedbackIdOrderByCreatedAtDesc(feedbackId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FeedbackAnalysisResponse toResponse(FeedbackAnalysis a) {
        return new FeedbackAnalysisResponse(
                a.getAnalysisId(),
                a.getFeedback().getFeedbackId(),
                a.getSentiment(),
                a.getCategory(),
                a.getTheme(),
                a.getEmotion(),
                a.getPriority(),
                a.getDepartment(),
                a.getShortSummary(),
                a.getConfidenceScore(),
                a.getModelName(),
                a.getModelVersion(),
                a.isCurrent(),
                a.getCreatedAt()
        );
    }
}
