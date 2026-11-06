-- ==============================================================================
-- Flyway Migration V2: Create Ingestion Schema
-- ==============================================================================

-- 1. Ingestion Batches Table
CREATE TABLE IF NOT EXISTS ingestion_batches (
    batch_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_file_name VARCHAR(255) NOT NULL,
    source_system VARCHAR(100) DEFAULT 'WEB_UPLOAD',
    upload_user VARCHAR(100),
    file_hash VARCHAR(64) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    successful_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    duplicate_rows INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_batches_status ON ingestion_batches(status);
CREATE INDEX IF NOT EXISTS idx_batches_created_at ON ingestion_batches(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_batches_file_hash ON ingestion_batches(file_hash);

-- 2. Customer Feedback Table
CREATE TABLE IF NOT EXISTS feedback (
    feedback_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES ingestion_batches(batch_id) ON DELETE CASCADE,
    feedback_date DATE NOT NULL,
    channel VARCHAR(100) NOT NULL,
    product VARCHAR(100) NOT NULL,
    rating INT CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5)),
    customer_segment VARCHAR(100),
    region VARCHAR(100),
    feedback_text TEXT NOT NULL,
    original_row_number INT NOT NULL,
    processing_status VARCHAR(50) NOT NULL DEFAULT 'STORED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_feedback_batch_id ON feedback(batch_id);
CREATE INDEX IF NOT EXISTS idx_feedback_date ON feedback(feedback_date DESC);
CREATE INDEX IF NOT EXISTS idx_feedback_channel ON feedback(channel);
CREATE INDEX IF NOT EXISTS idx_feedback_product ON feedback(product);
CREATE INDEX IF NOT EXISTS idx_feedback_rating ON feedback(rating);
CREATE INDEX IF NOT EXISTS idx_feedback_status ON feedback(processing_status);

-- 3. Processing Errors Table
CREATE TABLE IF NOT EXISTS processing_errors (
    error_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES ingestion_batches(batch_id) ON DELETE CASCADE,
    row_number INT,
    raw_record TEXT,
    error_type VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_errors_batch_id ON processing_errors(batch_id);
CREATE INDEX IF NOT EXISTS idx_errors_created_at ON processing_errors(created_at DESC);
