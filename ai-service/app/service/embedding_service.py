import logging
from typing import List, Optional
from uuid import UUID

from app.core.config import get_settings
from app.models.embedding import (
    EmbeddingResponse,
    BatchEmbeddingItemRequest,
    BatchEmbeddingItemResponse,
    BatchEmbeddingResponse,
)
from app.provider.embedding import EmbeddingProvider

logger = logging.getLogger(__name__)


class EmbeddingService:
    """
    Service responsible for coordinating vector embedding operations.
    Validates input texts, manages single/batch calls, and formats responses.
    """

    def __init__(self, provider: Optional[EmbeddingProvider] = None):
        self.provider = provider or EmbeddingProvider()
        settings = get_settings()
        self.default_model = settings.GEMINI_EMBEDDING_MODEL
        self.default_dimension = settings.GEMINI_EMBEDDING_DIMENSION

    def generate_single_embedding(
        self,
        text: str,
        dimension: Optional[int] = None,
        task_type: Optional[str] = "RETRIEVAL_DOCUMENT",
    ) -> EmbeddingResponse:
        target_dim = dimension or self.default_dimension
        values = self.provider.generate_embedding(
            text=text,
            dimension=target_dim,
            task_type=task_type,
        )
        return EmbeddingResponse(
            model=self.provider.model_name,
            dimension=len(values),
            embedding=values,
        )

    def generate_query_embedding(
        self,
        query: str,
        dimension: Optional[int] = None,
    ) -> EmbeddingResponse:
        return self.generate_single_embedding(
            text=query,
            dimension=dimension,
            task_type="RETRIEVAL_QUERY",
        )

    def generate_batch_embeddings(
        self,
        items: List[BatchEmbeddingItemRequest],
        dimension: Optional[int] = None,
    ) -> BatchEmbeddingResponse:
        target_dim = dimension or self.default_dimension
        results: List[BatchEmbeddingItemResponse] = []
        successful = 0
        failed = 0

        for item in items:
            try:
                values = self.provider.generate_embedding(
                    text=item.text,
                    dimension=target_dim,
                    task_type="RETRIEVAL_DOCUMENT",
                )
                results.append(
                    BatchEmbeddingItemResponse(
                        id=item.id,
                        model=self.provider.model_name,
                        dimension=len(values),
                        embedding=values,
                    )
                )
                successful += 1
            except Exception as e:
                logger.error("Failed to generate embedding for item %s: %s", item.id, e)
                failed += 1

        return BatchEmbeddingResponse(
            total=len(items),
            successful=successful,
            failed=failed,
            results=results,
        )
