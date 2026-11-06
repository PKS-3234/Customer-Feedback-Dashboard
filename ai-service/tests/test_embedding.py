import pytest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient

from app.main import app
from app.workflow.embedding_workflow import EmbeddingWorkflow
from app.provider.embedding import EmbeddingProvider
from app.service.embedding_service import EmbeddingService

client = TestClient(app)


def test_cosine_similarity_identical_vectors():
    vec_a = [1.0, 0.0, 0.0]
    vec_b = [1.0, 0.0, 0.0]
    sim = EmbeddingWorkflow.compute_cosine_similarity(vec_a, vec_b)
    assert pytest.approx(sim, 0.001) == 1.0


def test_cosine_similarity_orthogonal_vectors():
    vec_a = [1.0, 0.0, 0.0]
    vec_b = [0.0, 1.0, 0.0]
    sim = EmbeddingWorkflow.compute_cosine_similarity(vec_a, vec_b)
    assert pytest.approx(sim, 0.001) == 0.0


def test_cosine_similarity_opposite_vectors():
    vec_a = [1.0, 0.0]
    vec_b = [-1.0, 0.0]
    sim = EmbeddingWorkflow.compute_cosine_similarity(vec_a, vec_b)
    assert pytest.approx(sim, 0.001) == -1.0


def test_validate_text_rejects_empty():
    with pytest.raises(ValueError, match="Text cannot be empty or blank"):
        EmbeddingWorkflow.validate_text("   ")


def test_internal_generate_embedding_endpoint():
    dummy_vector = [0.1] * 768
    with patch.object(EmbeddingProvider, "generate_embedding", return_value=dummy_vector):
        response = client.post(
            "/internal/v1/embeddings/generate",
            json={"text": "Test feedback for biometric login", "dimension": 768},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["dimension"] == 768
        assert len(data["embedding"]) == 768
        assert data["model"] == "gemini-embedding-001"


def test_internal_query_embedding_endpoint():
    dummy_vector = [0.05] * 768
    with patch.object(EmbeddingProvider, "generate_embedding", return_value=dummy_vector):
        response = client.post(
            "/internal/v1/embeddings/query",
            json={"query": "duplicate card charge", "dimension": 768},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["dimension"] == 768
        assert len(data["embedding"]) == 768


def test_internal_batch_generate_embedding_endpoint():
    dummy_vector = [0.02] * 768
    with patch.object(EmbeddingProvider, "generate_embedding", return_value=dummy_vector):
        response = client.post(
            "/internal/v1/embeddings/batch-generate",
            json={
                "items": [
                    {"id": "a1111111-1111-1111-1111-111111111101", "text": "Instant money transfer is fast"},
                    {"id": "a1111111-1111-1111-1111-111111111102", "text": "Statements download smoothly"},
                ],
                "dimension": 768,
            },
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 2
        assert data["successful"] == 2
        assert data["failed"] == 0
        assert len(data["results"]) == 2
        assert data["results"][0]["dimension"] == 768
