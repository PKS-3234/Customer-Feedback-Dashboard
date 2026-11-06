import uuid
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient
import pytest

from app.main import app
from app.models.analysis import FeedbackAnalysisDTO
from app.provider.gemini import GeminiProvider

client = TestClient(app)


def test_schema_validation_valid():
    valid_data = {
        "sentiment": "POSITIVE",
        "category": "Mobile App",
        "theme": "Biometric Login",
        "emotion": "Happy",
        "priority": "LOW",
        "department": "Digital Banking",
        "short_summary": "Customer loves the fast fingerprint authentication.",
        "confidence_score": 0.96,
        "model_name": "gemini-2.5-flash",
        "model_version": "gemini-2.5-flash",
    }
    dto = FeedbackAnalysisDTO.model_validate(valid_data)
    assert dto.sentiment == "POSITIVE"
    assert dto.emotion == "Happy"
    assert dto.confidence_score == 0.96


def test_schema_validation_invalid_sentiment():
    invalid_data = {
        "sentiment": "ECSTATIC",  # Not allowed
        "category": "Mobile App",
        "theme": "Biometric Login",
        "emotion": "Happy",
        "priority": "LOW",
        "department": "Digital Banking",
        "short_summary": "Great app",
        "confidence_score": 0.96,
    }
    with pytest.raises(Exception):
        FeedbackAnalysisDTO.model_validate(invalid_data)


def test_schema_validation_confidence_out_of_bounds():
    invalid_data = {
        "sentiment": "POSITIVE",
        "category": "Mobile App",
        "theme": "Biometric Login",
        "emotion": "Happy",
        "priority": "LOW",
        "department": "Digital Banking",
        "short_summary": "Great app",
        "confidence_score": 1.5,  # Must be <= 1.0
    }
    with pytest.raises(Exception):
        FeedbackAnalysisDTO.model_validate(invalid_data)


@patch("app.provider.gemini.httpx.Client")
def test_gemini_provider_retry_logic(mock_client_cls):
    mock_client = MagicMock()
    mock_client_cls.return_value.__enter__.return_value = mock_client

    # First attempt fails with 500, second succeeds with valid json
    mock_fail = MagicMock()
    mock_fail.status_code = 500
    mock_fail.raise_for_status.side_effect = Exception("Internal Server Error")

    mock_success = MagicMock()
    mock_success.status_code = 200
    mock_success.raise_for_status.return_value = None
    mock_success.json.return_value = {
        "candidates": [
            {
                "content": {
                    "parts": [
                        {
                            "text": '{"sentiment":"NEGATIVE","category":"Transfers","theme":"Slow Transfer","emotion":"Frustrated","priority":"HIGH","department":"Payments Team","short_summary":"Transfer took 48 hours to clear.","confidence_score":0.92}'
                        }
                    ]
                }
            }
        ]
    }

    mock_client.post.side_effect = [mock_fail, mock_success]

    provider = GeminiProvider(api_key="test-api-key", retries=3)
    result = provider.analyze_feedback("Why did my transfer take two whole days?")

    assert result.sentiment == "NEGATIVE"
    assert result.priority == "HIGH"
    assert mock_client.post.call_count == 2


@patch("app.service.analysis_service.GeminiProvider.analyze_feedback")
def test_internal_analyze_endpoint(mock_analyze):
    mock_analyze.return_value = FeedbackAnalysisDTO(
        sentiment="POSITIVE",
        category="Cards",
        theme="Reward Redemption",
        emotion="Happy",
        priority="LOW",
        department="Customer Service",
        short_summary="Customer satisfied with card points exchange.",
        confidence_score=0.88,
        model_name="gemini-2.5-flash",
        model_version="gemini-2.5-flash",
    )

    feedback_id = str(uuid.uuid4())
    payload = {
        "feedback_id": feedback_id,
        "feedback_text": "I was able to convert my credit card reward points to airline miles easily.",
        "channel": "Mobile App",
        "product": "Credit Card",
        "rating": 5,
    }

    response = client.post("/internal/v1/analyze", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["feedback_id"] == feedback_id
    assert data["sentiment"] == "POSITIVE"
    assert data["category"] == "Cards"
    assert data["confidence_score"] == 0.88


@patch("app.service.analysis_service.GeminiProvider.analyze_feedback")
def test_internal_batch_analyze_endpoint(mock_analyze):
    mock_analyze.return_value = FeedbackAnalysisDTO(
        sentiment="NEUTRAL",
        category="Account Services",
        theme="Fee Inquiry",
        emotion="Neutral",
        priority="MEDIUM",
        department="Operations Team",
        short_summary="Customer inquired about monthly maintenance fees.",
        confidence_score=0.85,
        model_name="gemini-2.5-flash",
        model_version="gemini-2.5-flash",
    )

    feedback_id = str(uuid.uuid4())
    payload = {
        "items": [
            {
                "feedback_id": feedback_id,
                "feedback_text": "What is the minimum balance required to waive the maintenance fee?",
                "channel": "Web Portal",
                "product": "Checking Account",
                "rating": 3,
            }
        ]
    }

    response = client.post("/internal/v1/analyze/batch", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["total"] == 1
    assert data["successful"] == 1
    assert data["failed"] == 0
    assert len(data["results"]) == 1
    assert data["results"][0]["theme"] == "Fee Inquiry"
