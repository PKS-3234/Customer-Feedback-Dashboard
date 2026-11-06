import logging
from typing import Dict, Any, Optional
import psycopg

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class AnalyticsAgent:
    """
    Analytics Agent queries active feedback analyses from PostgreSQL
    to compute sentiment, category, theme, and priority distributions.
    """

    def __init__(self, db_url: Optional[str] = None):
        settings = get_settings()
        self.db_url = db_url or settings.db_connection_url

    def run(self, query: str, limit: int = 5) -> Dict[str, Any]:
        logger.info("AnalyticsAgent executing analysis distribution queries")
        try:
            with psycopg.connect(self.db_url) as conn:
                with conn.cursor() as cur:
                    # 1. Total active analyses
                    cur.execute("SELECT COUNT(*) FROM feedback_analysis WHERE is_current = true;")
                    total_count = cur.fetchone()[0]

                    if total_count == 0:
                        return {
                            "agent": "Analytics Agent",
                            "type": "ANALYTICS_METRICS",
                            "summary": "No active feedback analyses available in the system.",
                            "data": {
                                "total_analyzed": 0,
                                "sentiment_distribution": {},
                                "category_distribution": {},
                                "theme_distribution": {},
                                "priority_distribution": {},
                                "sample_analyses": []
                            }
                        }

                    # 2. Sentiment distribution
                    cur.execute("""
                        SELECT sentiment, COUNT(*) 
                        FROM feedback_analysis 
                        WHERE is_current = true 
                        GROUP BY sentiment 
                        ORDER BY COUNT(*) DESC;
                    """)
                    sentiment_dist = {row[0]: row[1] for row in cur.fetchall()}

                    # 3. Category distribution
                    cur.execute("""
                        SELECT category, COUNT(*) 
                        FROM feedback_analysis 
                        WHERE is_current = true 
                        GROUP BY category 
                        ORDER BY COUNT(*) DESC;
                    """)
                    category_dist = {row[0]: row[1] for row in cur.fetchall()}

                    # 4. Theme distribution
                    cur.execute("""
                        SELECT theme, COUNT(*) 
                        FROM feedback_analysis 
                        WHERE is_current = true 
                        GROUP BY theme 
                        ORDER BY COUNT(*) DESC 
                        LIMIT 10;
                    """)
                    theme_dist = {row[0]: row[1] for row in cur.fetchall()}

                    # 5. Priority distribution
                    cur.execute("""
                        SELECT priority, COUNT(*) 
                        FROM feedback_analysis 
                        WHERE is_current = true 
                        GROUP BY priority 
                        ORDER BY COUNT(*) DESC;
                    """)
                    priority_dist = {row[0]: row[1] for row in cur.fetchall()}

                    # 6. Sample records
                    cur.execute("""
                        SELECT fa.sentiment, fa.category, fa.theme, fa.priority, fa.short_summary, f.feedback_text
                        FROM feedback_analysis fa
                        JOIN feedback f ON fa.feedback_id = f.feedback_id
                        WHERE fa.is_current = true
                        ORDER BY 
                            CASE fa.priority 
                                WHEN 'CRITICAL' THEN 1 
                                WHEN 'HIGH' THEN 2 
                                WHEN 'MEDIUM' THEN 3 
                                ELSE 4 
                            END, fa.created_at DESC
                        LIMIT %s;
                    """, (limit,))
                    samples = [
                        {
                            "sentiment": row[0],
                            "category": row[1],
                            "theme": row[2],
                            "priority": row[3],
                            "short_summary": row[4],
                            "feedback_text": row[5]
                        }
                        for row in cur.fetchall()
                    ]

            return {
                "agent": "Analytics Agent",
                "type": "ANALYTICS_METRICS",
                "summary": f"Analyzed {total_count} records. Sentiments: {sentiment_dist}. Top categories: {list(category_dist.keys())[:3]}.",
                "data": {
                    "total_analyzed": total_count,
                    "sentiment_distribution": sentiment_dist,
                    "category_distribution": category_dist,
                    "theme_distribution": theme_dist,
                    "priority_distribution": priority_dist,
                    "sample_analyses": samples
                }
            }
        except Exception as e:
            logger.error("AnalyticsAgent failed to query database: %s", e)
            return {
                "agent": "Analytics Agent",
                "type": "ANALYTICS_ERROR",
                "summary": f"Could not fetch analytics metrics: {str(e)}",
                "data": {}
            }
