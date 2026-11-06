package com.customerfeedback.platform.entity;

import com.customerfeedback.platform.model.enums.BatchStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingestion_batches")
public class IngestionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "batch_id", updatable = false, nullable = false)
    private UUID batchId;

    @Column(name = "source_file_name", nullable = false)
    private String sourceFileName;

    @Column(name = "source_system")
    private String sourceSystem = "WEB_UPLOAD";

    @Column(name = "upload_user")
    private String uploadUser;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "total_rows", nullable = false)
    private int totalRows = 0;

    @Column(name = "successful_rows", nullable = false)
    private int successfulRows = 0;

    @Column(name = "failed_rows", nullable = false)
    private int failedRows = 0;

    @Column(name = "duplicate_rows", nullable = false)
    private int duplicateRows = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BatchStatus status = BatchStatus.PROCESSING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.sourceSystem == null) {
            this.sourceSystem = "WEB_UPLOAD";
        }
    }

    public IngestionBatch() {}

    public IngestionBatch(String sourceFileName, String sourceSystem, String uploadUser, String fileHash) {
        this.sourceFileName = sourceFileName;
        this.sourceSystem = sourceSystem != null ? sourceSystem : "WEB_UPLOAD";
        this.uploadUser = uploadUser;
        this.fileHash = fileHash;
        this.status = BatchStatus.PROCESSING;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getUploadUser() {
        return uploadUser;
    }

    public void setUploadUser(String uploadUser) {
        this.uploadUser = uploadUser;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessfulRows() {
        return successfulRows;
    }

    public void setSuccessfulRows(int successfulRows) {
        this.successfulRows = successfulRows;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(int failedRows) {
        this.failedRows = failedRows;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public void setDuplicateRows(int duplicateRows) {
        this.duplicateRows = duplicateRows;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
