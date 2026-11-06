import logging
from typing import List
from app.models.analysis import (
    AnalyzeFeedbackRequest,
    AnalysisResponse,
    BatchAnalyzeRequest,
    BatchAnalyzeResponse,
)
from app.provider.gemini import GeminiProvider

logger = logging.getLogger(__name__)


class AnalysisService:

    def __init__(self, provider: GeminiProvider | None = None):
        self.provider = provider or GeminiProvider()

    def analyze_single(self, request: AnalyzeFeedbackRequest) -> AnalysisResponse:
        metadata = {
            "channel": request.channel,
            "product": request.product,
            "rating": request.rating,
            "customer_segment": request.customer_segment,
            "region": request.region,
        }

        dto = self.provider.analyze_feedback(
            feedback_text=request.feedback_text,
            metadata=metadata,
        )

        return AnalysisResponse(
            feedback_id=request.feedback_id,
            sentiment=dto.sentiment,
            category=dto.category,
            theme=dto.theme,
            emotion=dto.emotion,
            priority=dto.priority,
            department=dto.department,
            short_summary=dto.short_summary,
            confidence_score=dto.confidence_score,
            model_name=dto.model_name,
            model_version=dto.model_version,
        )

    def analyze_batch(self, request: BatchAnalyzeRequest) -> BatchAnalyzeResponse:
        total = len(request.items)
        successful = 0
        failed = 0
        results: List[AnalysisResponse] = []

        for item in request.items:
            try:
                res = self.analyze_single(item)
                results.append(res)
                successful += 1
            except Exception as e:
                logger.error("Failed to analyze item %s: %s", item.feedback_id, e)
                failed += 1

        return BatchAnalyzeResponse(
            total=total,
            successful=successful,
            failed=failed,
            results=results,
        )
