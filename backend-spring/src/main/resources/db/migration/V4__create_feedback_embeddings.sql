-- ==============================================================================
-- Migration: V4__create_feedback_embeddings.sql
-- Description: Creates table for storing Gemini feedback embeddings with PGVector
--              HNSW index using cosine similarity for high-performance semantic search.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS feedback_embeddings (
    embedding_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id         UUID NOT NULL REFERENCES feedback(feedback_id) ON DELETE CASCADE,
    analysis_id         UUID NOT NULL REFERENCES feedback_analysis(analysis_id) ON DELETE CASCADE,
    embedding_model     VARCHAR(100) NOT NULL,
    embedding_dimension INT NOT NULL,
    embedding           vector(768) NOT NULL,
    is_current          BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
);

-- HNSW index for fast approximate nearest neighbor semantic search with cosine distance (<=>)
CREATE INDEX IF NOT EXISTS idx_embeddings_hnsw 
    ON feedback_embeddings USING hnsw (embedding vector_cosine_ops);

-- Operational indexes for versioning and lookups
CREATE INDEX IF NOT EXISTS idx_embeddings_feedback_id ON feedback_embeddings(feedback_id);
CREATE INDEX IF NOT EXISTS idx_embeddings_analysis_id ON feedback_embeddings(analysis_id);
CREATE INDEX IF NOT EXISTS idx_embeddings_is_current ON feedback_embeddings(is_current);
CREATE INDEX IF NOT EXISTS idx_embeddings_created_at ON feedback_embeddings(created_at DESC);
