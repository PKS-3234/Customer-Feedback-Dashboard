package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.dto.LabelCountProjection;
import com.customerfeedback.platform.entity.FeedbackAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackAnalysisRepository extends JpaRepository<FeedbackAnalysis, UUID> {

    Optional<FeedbackAnalysis> findByFeedback_FeedbackIdAndIsCurrentTrue(UUID feedbackId);

    List<FeedbackAnalysis> findByFeedback_Batch_BatchIdAndIsCurrentTrueOrderByCreatedAtDesc(UUID batchId);

    List<FeedbackAnalysis> findByFeedback_FeedbackIdOrderByCreatedAtDesc(UUID feedbackId);

    boolean existsByFeedback_FeedbackIdAndIsCurrentTrue(UUID feedbackId);

    long countByIsCurrentTrue();

    long countByIsCurrentTrueAndSentimentIgnoreCase(String sentiment);

    long countByIsCurrentTrueAndPriorityIgnoreCase(String priority);

    @Modifying
    @Query("UPDATE FeedbackAnalysis fa SET fa.isCurrent = false WHERE fa.feedback.feedbackId = :feedbackId AND fa.isCurrent = true")
    void markPriorAnalysesNotCurrent(@Param("feedbackId") UUID feedbackId);

    @Query("SELECT fa.sentiment AS label, COUNT(fa) AS count FROM FeedbackAnalysis fa WHERE fa.isCurrent = true GROUP BY fa.sentiment ORDER BY COUNT(fa) DESC")
    List<LabelCountProjection> findSentimentDistribution();

    @Query("SELECT fa.category AS label, COUNT(fa) AS count FROM FeedbackAnalysis fa WHERE fa.isCurrent = true GROUP BY fa.category ORDER BY COUNT(fa) DESC")
    List<LabelCountProjection> findCategoryDistribution();

    @Query("SELECT fa.theme AS label, COUNT(fa) AS count FROM FeedbackAnalysis fa WHERE fa.isCurrent = true GROUP BY fa.theme ORDER BY COUNT(fa) DESC")
    List<LabelCountProjection> findThemeDistribution();

    @Query("SELECT fa.priority AS label, COUNT(fa) AS count FROM FeedbackAnalysis fa WHERE fa.isCurrent = true GROUP BY fa.priority ORDER BY COUNT(fa) DESC")
    List<LabelCountProjection> findPriorityDistribution();
}
