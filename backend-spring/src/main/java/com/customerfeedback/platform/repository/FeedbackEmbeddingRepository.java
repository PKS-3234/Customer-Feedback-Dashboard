package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.dto.SemanticSearchProjection;
import com.customerfeedback.platform.entity.FeedbackEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackEmbeddingRepository extends JpaRepository<FeedbackEmbedding, UUID> {

    Optional<FeedbackEmbedding> findByFeedback_FeedbackIdAndIsCurrentTrue(UUID feedbackId);

    Optional<FeedbackEmbedding> findByAnalysis_AnalysisIdAndIsCurrentTrue(UUID analysisId);

    boolean existsByAnalysis_AnalysisIdAndIsCurrentTrue(UUID analysisId);

    long countByIsCurrentTrue();

    Optional<FeedbackEmbedding> findFirstByIsCurrentTrueOrderByCreatedAtDesc();

    @Modifying
    @Transactional
    @Query("UPDATE FeedbackEmbedding fe SET fe.isCurrent = false WHERE fe.feedback.feedbackId = :feedbackId AND fe.isCurrent = true")
    void markPriorEmbeddingsNotCurrent(@Param("feedbackId") UUID feedbackId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO feedback_embeddings (
            embedding_id, feedback_id, analysis_id, embedding_model, embedding_dimension, embedding, is_current, created_at
        ) VALUES (
            :id, :feedbackId, :analysisId, :model, :dimension, CAST(:vectorStr AS vector), :isCurrent, :createdAt
        )
    """, nativeQuery = true)
    void insertEmbedding(
            @Param("id") UUID id,
            @Param("feedbackId") UUID feedbackId,
            @Param("analysisId") UUID analysisId,
            @Param("model") String model,
            @Param("dimension") int dimension,
            @Param("vectorStr") String vectorStr,
            @Param("isCurrent") boolean isCurrent,
            @Param("createdAt") Instant createdAt
    );

    @Query(value = """
        SELECT f.feedback_id AS feedbackId,
               f.feedback_text AS feedbackText,
               ROUND(CAST(1.0 - (fe.embedding <=> CAST(:queryVector AS vector)) AS numeric), 4) AS similarity,
               fa.category AS category,
               fa.theme AS theme,
               fa.sentiment AS sentiment,
               fa.priority AS priority
        FROM feedback_embeddings fe
        JOIN feedback f ON fe.feedback_id = f.feedback_id
        JOIN feedback_analysis fa ON fe.analysis_id = fa.analysis_id
        WHERE fe.is_current = true
        ORDER BY fe.embedding <=> CAST(:queryVector AS vector) ASC
        LIMIT :limit
    """, nativeQuery = true)
    List<SemanticSearchProjection> searchByCosineSimilarity(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );
}
