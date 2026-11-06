import logging
from typing import Dict, Any, Optional
import psycopg

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class DataQualityAgent:
    """
    Data Quality Agent analyzes ingestion batches, processing errors,
    analysis coverage, and vector embedding coverage across the platform.
    """

    def __init__(self, db_url: Optional[str] = None):
        settings = get_settings()
        self.db_url = db_url or settings.db_connection_url

    def run(self, query: str, limit: int = 5) -> Dict[str, Any]:
        logger.info("DataQualityAgent gathering system ingestion and coverage metrics")
        try:
            with psycopg.connect(self.db_url) as conn:
                with conn.cursor() as cur:
                    # 1. Batch metrics
                    cur.execute("""
                        SELECT COUNT(*),
                               COALESCE(SUM(total_rows), 0),
                               COALESCE(SUM(successful_rows), 0),
                               COALESCE(SUM(failed_rows), 0),
                               COALESCE(SUM(duplicate_rows), 0)
                        FROM ingestion_batches;
                    """)
                    b_row = cur.fetchone()
                    total_batches = b_row[0]
                    total_rows = b_row[1]
                    successful_rows = b_row[2]
                    failed_rows = b_row[3]
                    duplicate_rows = b_row[4]

                    # 2. Total stored feedback records
                    cur.execute("SELECT COUNT(*) FROM feedback;")
                    total_feedback = cur.fetchone()[0]

                    # 3. Active analysis coverage
                    cur.execute("SELECT COUNT(*) FROM feedback_analysis WHERE is_current = true;")
                    active_analyses = cur.fetchone()[0]
                    analysis_coverage_pct = round(
                        (active_analyses / total_feedback * 100.0) if total_feedback > 0 else 0.0, 1
                    )

                    # 4. Active embedding coverage
                    cur.execute("SELECT COUNT(*) FROM feedback_embeddings WHERE is_current = true;")
                    active_embeddings = cur.fetchone()[0]
                    embedding_coverage_pct = round(
                        (active_embeddings / active_analyses * 100.0) if active_analyses > 0 else 0.0, 1
                    )

                    # 5. Error counts and breakdown
                    cur.execute("SELECT COUNT(*) FROM processing_errors;")
                    total_errors = cur.fetchone()[0]

                    cur.execute("""
                        SELECT error_type, COUNT(*)
                        FROM processing_errors
                        GROUP BY error_type
                        ORDER BY COUNT(*) DESC;
                    """)
                    error_breakdown = {row[0]: row[1] for row in cur.fetchall()}

                    # 6. Sample recent errors
                    cur.execute("""
                        SELECT row_number, error_type, error_message
                        FROM processing_errors
                        ORDER BY created_at DESC
                        LIMIT %s;
                    """, (limit,))
                    sample_errors = [
                        {
                            "row_number": row[0],
                            "error_type": row[1],
                            "error_message": row[2]
                        }
                        for row in cur.fetchall()
                    ]

            summary_text = (
                f"Ingested {total_batches} batches with {total_feedback} stored feedback records. "
                f"Analysis Coverage: {analysis_coverage_pct}% ({active_analyses}/{total_feedback}). "
                f"Embedding Coverage: {embedding_coverage_pct}% ({active_embeddings}/{active_analyses}). "
                f"Processing Failures: {total_errors}, Duplicates: {duplicate_rows}."
            )

            return {
                "agent": "Data Quality Agent",
                "type": "DATA_QUALITY_METRICS",
                "summary": summary_text,
                "data": {
                    "total_batches": total_batches,
                    "total_rows_uploaded": total_rows,
                    "successful_rows": successful_rows,
                    "failed_rows": failed_rows,
                    "duplicate_rows": duplicate_rows,
                    "total_feedback_stored": total_feedback,
                    "active_analyses": active_analyses,
                    "analysis_coverage_percent": analysis_coverage_pct,
                    "active_embeddings": active_embeddings,
                    "embedding_coverage_percent": embedding_coverage_pct,
                    "total_processing_errors": total_errors,
                    "error_breakdown": error_breakdown,
                    "recent_errors": sample_errors
                }
            }
        except Exception as e:
            logger.error("DataQualityAgent failed: %s", e)
            return {
                "agent": "Data Quality Agent",
                "type": "DATA_QUALITY_ERROR",
                "summary": f"Data quality query failed: {str(e)}",
                "data": {}
            }
