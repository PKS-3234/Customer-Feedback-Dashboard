import logging
from typing import Dict, Any, List, Optional
import psycopg

from app.core.config import get_settings
from app.provider.embedding import EmbeddingProvider

logger = logging.getLogger(__name__)


class RetrievalAgent:
    """
    Retrieval Agent generates query embeddings and performs PGVector semantic search
    to gather supporting, representative customer quotes and complaints.
    """

    def __init__(self, provider: Optional[EmbeddingProvider] = None, db_url: Optional[str] = None):
        settings = get_settings()
        self.provider = provider or EmbeddingProvider()
        self.db_url = db_url or settings.db_connection_url

    def run(self, query: str, limit: int = 5) -> Dict[str, Any]:
        logger.info("RetrievalAgent searching for semantically relevant feedback for: '%s'", query)
        try:
            # 1. Generate query embedding vector
            values = self.provider.generate_embedding(
                text=query,
                dimension=768,
                task_type="RETRIEVAL_QUERY"
            )
            vector_str = "[" + ", ".join(f"{v:.8f}" for v in values) + "]"

            # 2. Perform PGVector similarity search
            with psycopg.connect(self.db_url) as conn:
                with conn.cursor() as cur:
                    cur.execute("""
                        SELECT f.feedback_id,
                               f.feedback_text,
                               ROUND(CAST(1.0 - (fe.embedding <=> %s::vector) AS numeric), 4) AS similarity,
                               fa.category,
                               fa.theme,
                               fa.sentiment,
                               fa.priority
                        FROM feedback_embeddings fe
                        JOIN feedback f ON fe.feedback_id = f.feedback_id
                        JOIN feedback_analysis fa ON fe.analysis_id = fa.analysis_id
                        WHERE fe.is_current = true
                        ORDER BY fe.embedding <=> %s::vector ASC
                        LIMIT %s;
                    """, (vector_str, vector_str, limit))

                    rows = cur.fetchall()
                    matches = [
                        {
                            "feedback_id": str(r[0]),
                            "feedback_text": r[1],
                            "similarity": float(r[2]),
                            "category": r[3],
                            "theme": r[4],
                            "sentiment": r[5],
                            "priority": r[6]
                        }
                        for r in rows
                    ]

            return {
                "agent": "Retrieval Agent",
                "type": "SEMANTIC_EVIDENCE",
                "summary": f"Retrieved {len(matches)} semantically matched feedback items (similarity range: {matches[0]['similarity'] if matches else 0} to {matches[-1]['similarity'] if matches else 0}).",
                "data": matches
            }
        except Exception as e:
            logger.error("RetrievalAgent failed: %s", e)
            return {
                "agent": "Retrieval Agent",
                "type": "RETRIEVAL_ERROR",
                "summary": f"Semantic search retrieval failed: {str(e)}",
                "data": []
            }
