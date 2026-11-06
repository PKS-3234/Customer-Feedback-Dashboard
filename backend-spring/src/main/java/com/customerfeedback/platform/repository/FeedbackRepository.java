package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.dto.LabelCountProjection;
import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.model.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    Page<Feedback> findByBatch_BatchIdOrderByOriginalRowNumberAsc(UUID batchId, Pageable pageable);

    List<Feedback> findByBatch_BatchIdOrderByOriginalRowNumberAsc(UUID batchId);

    List<Feedback> findByBatch_BatchIdAndProcessingStatus(UUID batchId, ProcessingStatus processingStatus);

    boolean existsByFeedbackId(UUID feedbackId);

    boolean existsByBatch_BatchIdAndFeedbackText(UUID batchId, String feedbackText);

    long countByBatch_BatchId(UUID batchId);

    @Query("SELECT f.channel AS label, COUNT(f) AS count FROM Feedback f GROUP BY f.channel ORDER BY COUNT(f) DESC")
    List<LabelCountProjection> findChannelDistribution();

    @Query("SELECT f.product AS label, COUNT(f) AS count FROM Feedback f GROUP BY f.product ORDER BY COUNT(f) DESC")
    List<LabelCountProjection> findProductDistribution();
}
