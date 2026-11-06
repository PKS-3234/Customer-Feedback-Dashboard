package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.entity.IngestionBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngestionBatchRepository extends JpaRepository<IngestionBatch, UUID> {

    Optional<IngestionBatch> findTopByFileHashOrderByCreatedAtDesc(String fileHash);

    Page<IngestionBatch> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.failedRows), 0) FROM IngestionBatch b")
    long sumFailedRows();

    @Query("SELECT COALESCE(SUM(b.duplicateRows), 0) FROM IngestionBatch b")
    long sumDuplicateRows();
}
