-- ==============================================================================
-- Flyway Migration V3: Create Feedback Analysis Table
-- ==============================================================================
-- Stores versioned structured AI analysis results from Google Gemini.
-- Preserves full analysis history while enforcing single current active record.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS feedback_analysis (
    analysis_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL REFERENCES feedback(feedback_id) ON DELETE CASCADE,
    sentiment VARCHAR(20) NOT NULL,
    category VARCHAR(100) NOT NULL,
    theme VARCHAR(100) NOT NULL,
    emotion VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    department VARCHAR(100) NOT NULL,
    short_summary TEXT NOT NULL,
    confidence_score NUMERIC(3, 2) NOT NULL CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_analysis_feedback_id ON feedback_analysis(feedback_id);
CREATE INDEX IF NOT EXISTS idx_analysis_is_current ON feedback_analysis(is_current);
CREATE INDEX IF NOT EXISTS idx_analysis_sentiment ON feedback_analysis(sentiment);
CREATE INDEX IF NOT EXISTS idx_analysis_category ON feedback_analysis(category);
CREATE INDEX IF NOT EXISTS idx_analysis_priority ON feedback_analysis(priority);
CREATE INDEX IF NOT EXISTS idx_analysis_created_at ON feedback_analysis(created_at DESC);
