package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.dto.LabelCountProjection;
import com.customerfeedback.platform.entity.ProcessingError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessingErrorRepository extends JpaRepository<ProcessingError, UUID> {

    Page<ProcessingError> findByBatch_BatchIdOrderByRowNumberAsc(UUID batchId, Pageable pageable);

    long countByBatch_BatchId(UUID batchId);

    @Query("SELECT pe.errorType AS label, COUNT(pe) AS count FROM ProcessingError pe GROUP BY pe.errorType ORDER BY COUNT(pe) DESC")
    List<LabelCountProjection> findErrorDistribution();
}
