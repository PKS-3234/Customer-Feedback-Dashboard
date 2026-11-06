-- ==============================================================================
-- Customer Feedback Intelligence Platform
-- Database Initialization Script (Phase 1)
-- ==============================================================================
-- Enables the PGVector extension for embedding vectors and UUID extension for primary keys.
-- Application schemas, ingestion_batches, feedback, feedback_analysis, etc.
-- will be introduced in subsequent phases.
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Log verification in PostgreSQL
DO $$
BEGIN
    RAISE NOTICE 'PGVector extension enabled: %', (SELECT extversion FROM pg_extension WHERE extname = 'vector');
    RAISE NOTICE 'UUID extension enabled: %', (SELECT extversion FROM pg_extension WHERE extname = 'uuid-ossp');
END $$;
