import math
import logging
from typing import List, Dict, Any, Optional

from app.service.embedding_service import EmbeddingService
from app.models.embedding import EmbeddingResponse

logger = logging.getLogger(__name__)


class EmbeddingWorkflow:
    """
    Workflow orchestrator for embedding generation, validation, and semantic similarity scoring.
    """

    def __init__(self, service: Optional[EmbeddingService] = None):
        self.service = service or EmbeddingService()

    def process_document_embedding(self, text: str, dimension: Optional[int] = None) -> EmbeddingResponse:
        self.validate_text(text)
        return self.service.generate_single_embedding(text, dimension=dimension)

    def process_query_embedding(self, query: str, dimension: Optional[int] = None) -> EmbeddingResponse:
        self.validate_text(query)
        return self.service.generate_query_embedding(query, dimension=dimension)

    @staticmethod
    def validate_text(text: str) -> None:
        if not text or not text.strip():
            raise ValueError("Text cannot be empty or blank for embedding workflow.")
        if len(text.strip()) > 10000:
            raise ValueError("Text exceeds maximum permissible character length of 10,000.")

    @staticmethod
    def compute_cosine_similarity(vec_a: List[float], vec_b: List[float]) -> float:
        """
        Compute standard cosine similarity between two float vectors.
        """
        if len(vec_a) != len(vec_b):
            raise ValueError(f"Vector dimensions do not match: {len(vec_a)} vs {len(vec_b)}")

        dot_product = sum(a * b for a, b in zip(vec_a, vec_b))
        norm_a = math.sqrt(sum(a * a for a in vec_a))
        norm_b = math.sqrt(sum(b * b for b in vec_b))

        if norm_a == 0.0 or norm_b == 0.0:
            return 0.0

        similarity = dot_product / (norm_a * norm_b)
        # Clamp to [-1.0, 1.0] to prevent floating point inaccuracies
        return max(-1.0, min(1.0, similarity))

    def rank_candidates(
        self,
        query: str,
        candidates: List[Dict[str, Any]],
        dimension: Optional[int] = None,
    ) -> List[Dict[str, Any]]:
        """
        Calculates embeddings and ranks candidates in descending order of cosine similarity.
        """
        if not candidates:
            return []

        query_emb = self.process_query_embedding(query, dimension=dimension).embedding
        scored_results = []

        for candidate in candidates:
            cand_text = candidate.get("text", "")
            cand_emb = self.process_document_embedding(cand_text, dimension=dimension).embedding
            sim = self.compute_cosine_similarity(query_emb, cand_emb)
            res = dict(candidate)
            res["similarity"] = round(sim, 4)
            scored_results.append(res)

        scored_results.sort(key=lambda x: x["similarity"], reverse=True)
        return scored_results
