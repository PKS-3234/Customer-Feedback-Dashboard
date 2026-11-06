package com.customerfeedback.platform.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processing_errors")
public class ProcessingError {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "error_id", updatable = false, nullable = false)
    private UUID errorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private IngestionBatch batch;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "raw_record", columnDefinition = "TEXT")
    private String rawRecord;

    @Column(name = "error_type", nullable = false, length = 100)
    private String errorType;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public ProcessingError() {}

    public ProcessingError(IngestionBatch batch, Integer rowNumber, String rawRecord, String errorType, String errorMessage) {
        this.batch = batch;
        this.rowNumber = rowNumber;
        this.rawRecord = rawRecord;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public UUID getErrorId() {
        return errorId;
    }

    public void setErrorId(UUID errorId) {
        this.errorId = errorId;
    }

    public IngestionBatch getBatch() {
        return batch;
    }

    public void setBatch(IngestionBatch batch) {
        this.batch = batch;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getRawRecord() {
        return rawRecord;
    }

    public void setRawRecord(String rawRecord) {
        this.rawRecord = rawRecord;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
