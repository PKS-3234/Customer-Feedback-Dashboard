package com.customerfeedback.platform.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedback_embeddings")
public class FeedbackEmbedding {

    @Id
    @Column(name = "embedding_id", nullable = false, updatable = false)
    private UUID embeddingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private FeedbackAnalysis analysis;

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @Column(name = "embedding_dimension", nullable = false)
    private Integer embeddingDimension;

    @Column(name = "embedding", columnDefinition = "vector")
    private String embedding;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FeedbackEmbedding() {
    }

    public FeedbackEmbedding(UUID embeddingId, Feedback feedback, FeedbackAnalysis analysis,
                             String embeddingModel, Integer embeddingDimension, String embedding,
                             Boolean isCurrent, Instant createdAt) {
        this.embeddingId = embeddingId;
        this.feedback = feedback;
        this.analysis = analysis;
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        this.embedding = embedding;
        this.isCurrent = isCurrent;
        this.createdAt = createdAt;
    }

    public UUID getEmbeddingId() {
        return embeddingId;
    }

    public void setEmbeddingId(UUID embeddingId) {
        this.embeddingId = embeddingId;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public FeedbackAnalysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(FeedbackAnalysis analysis) {
        this.analysis = analysis;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
