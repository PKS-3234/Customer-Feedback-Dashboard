package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.*;
import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.entity.FeedbackAnalysis;
import com.customerfeedback.platform.entity.FeedbackEmbedding;
import com.customerfeedback.platform.exception.ResourceNotFoundException;
import com.customerfeedback.platform.model.enums.ProcessingStatus;
import com.customerfeedback.platform.repository.FeedbackAnalysisRepository;
import com.customerfeedback.platform.repository.FeedbackEmbeddingRepository;
import com.customerfeedback.platform.repository.FeedbackRepository;
import com.customerfeedback.platform.repository.IngestionBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final FeedbackEmbeddingRepository embeddingRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackAnalysisRepository analysisRepository;
    private final IngestionBatchRepository batchRepository;
    private final AiEmbeddingClient aiEmbeddingClient;

    public EmbeddingService(
            FeedbackEmbeddingRepository embeddingRepository,
            FeedbackRepository feedbackRepository,
            FeedbackAnalysisRepository analysisRepository,
            IngestionBatchRepository batchRepository,
            AiEmbeddingClient aiEmbeddingClient) {
        this.embeddingRepository = embeddingRepository;
        this.feedbackRepository = feedbackRepository;
        this.analysisRepository = analysisRepository;
        this.batchRepository = batchRepository;
        this.aiEmbeddingClient = aiEmbeddingClient;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FeedbackEmbeddingResponse generateFeedbackEmbedding(UUID feedbackId, boolean force) {
        log.info("Generating embedding for feedback ID: {}, force={}", feedbackId, force);

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback record not found: " + feedbackId));

        FeedbackAnalysis currentAnalysis = analysisRepository.findByFeedback_FeedbackIdAndIsCurrentTrue(feedbackId)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot generate embedding: feedback record " + feedbackId + " has no active analysis."
                ));

        // Free Tier Constraint: Skip generation if embedding already exists for current analysis version
        if (!force && embeddingRepository.existsByAnalysis_AnalysisIdAndIsCurrentTrue(currentAnalysis.getAnalysisId())) {
            log.info("Active embedding already exists for analysis ID {}. Skipping generation.", currentAnalysis.getAnalysisId());
            FeedbackEmbedding existing = embeddingRepository
                    .findByAnalysis_AnalysisIdAndIsCurrentTrue(currentAnalysis.getAnalysisId())
                    .orElseThrow();
            return toResponse(existing);
        }

        // Call Gemini embedding provider via AI service
        AiEmbeddingClient.EmbeddingResult result = aiEmbeddingClient.requestEmbedding(feedback.getFeedbackText());

        // Versioning: Mark any previous embeddings for this feedback record as is_current = false
        embeddingRepository.markPriorEmbeddingsNotCurrent(feedbackId);

        // Insert new embedding vector linked to the current analysis version
        UUID embeddingId = UUID.randomUUID();
        Instant now = Instant.now();

        embeddingRepository.insertEmbedding(
                embeddingId,
                feedback.getFeedbackId(),
                currentAnalysis.getAnalysisId(),
                result.model(),
                result.dimension(),
                result.vectorString(),
                true,
                now
        );

        return new FeedbackEmbeddingResponse(
                embeddingId,
                feedback.getFeedbackId(),
                currentAnalysis.getAnalysisId(),
                result.model(),
                result.dimension(),
                true,
                now
        );
    }

    @Transactional
    public BatchEmbeddingSummaryResponse generateBatchEmbeddings(UUID batchId, int limit, boolean force) {
        log.info("Starting batch embedding generation for batch ID: {}, limit={}, force={}", batchId, limit, force);

        if (!batchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("Batch not found with ID: " + batchId);
        }

        int effectiveLimit = Math.min(Math.max(limit, 1), 100);
        List<Feedback> batchFeedbacks = feedbackRepository
                .findByBatch_BatchIdAndProcessingStatus(batchId, ProcessingStatus.STORED);

        List<Feedback> targetFeedbacks = batchFeedbacks.stream()
                .limit(effectiveLimit)
                .toList();

        List<FeedbackEmbeddingResponse> results = new ArrayList<>();
        int generatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Feedback fb : targetFeedbacks) {
            try {
                var analysisOpt = analysisRepository.findByFeedback_FeedbackIdAndIsCurrentTrue(fb.getFeedbackId());
                if (analysisOpt.isEmpty()) {
                    log.warn("Feedback ID {} has no active analysis; skipping embedding.", fb.getFeedbackId());
                    skippedCount++;
                    continue;
                }

                UUID analysisId = analysisOpt.get().getAnalysisId();
                if (!force && embeddingRepository.existsByAnalysis_AnalysisIdAndIsCurrentTrue(analysisId)) {
                    skippedCount++;
                    FeedbackEmbedding existing = embeddingRepository
                            .findByAnalysis_AnalysisIdAndIsCurrentTrue(analysisId).orElseThrow();
                    results.add(toResponse(existing));
                    continue;
                }

                FeedbackEmbeddingResponse created = generateFeedbackEmbedding(fb.getFeedbackId(), force);
                results.add(created);
                generatedCount++;
            } catch (Exception ex) {
                log.error("Failed to generate embedding for feedback ID {}: {}", fb.getFeedbackId(), ex.getMessage());
                failedCount++;
            }
        }

        return new BatchEmbeddingSummaryResponse(
                batchId,
                targetFeedbacks.size(),
                generatedCount,
                skippedCount,
                failedCount,
                results
        );
    }

    @Transactional(readOnly = true)
    public FeedbackEmbeddingResponse getFeedbackEmbedding(UUID feedbackId) {
        FeedbackEmbedding embedding = embeddingRepository.findByFeedback_FeedbackIdAndIsCurrentTrue(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Active embedding not found for feedback ID: " + feedbackId));
        return toResponse(embedding);
    }

    @Transactional(readOnly = true)
    public List<SemanticSearchResultResponse> performSemanticSearch(String query, int limit) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);
        log.info("Executing semantic search for query: '{}', limit: {}", query, effectiveLimit);

        // 1. Generate query embedding from AI Service
        String queryVectorStr = aiEmbeddingClient.requestQueryEmbedding(query);

        // 2. Perform PGVector cosine similarity search
        List<SemanticSearchProjection> projections = embeddingRepository.searchByCosineSimilarity(
                queryVectorStr,
                effectiveLimit
        );

        return projections.stream()
                .map(p -> new SemanticSearchResultResponse(
                        p.getFeedbackId(),
                        p.getFeedbackText(),
                        p.getSimilarity(),
                        p.getCategory(),
                        p.getTheme(),
                        p.getSentiment(),
                        p.getPriority()
                ))
                .toList();
    }

    private FeedbackEmbeddingResponse toResponse(FeedbackEmbedding entity) {
        return new FeedbackEmbeddingResponse(
                entity.getEmbeddingId(),
                entity.getFeedback().getFeedbackId(),
                entity.getAnalysis().getAnalysisId(),
                entity.getEmbeddingModel(),
                entity.getEmbeddingDimension(),
                entity.getIsCurrent(),
                entity.getCreatedAt()
        );
    }
}
