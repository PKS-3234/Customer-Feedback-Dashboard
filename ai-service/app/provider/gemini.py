import json
import logging
import time
from typing import Optional, Dict, Any
import httpx
from pydantic import ValidationError

from app.core.config import get_settings
from app.models.analysis import FeedbackAnalysisDTO

logger = logging.getLogger(__name__)

GEMINI_ANALYSIS_SCHEMA = {
    "type": "OBJECT",
    "properties": {
        "sentiment": {
            "type": "STRING",
            "enum": ["POSITIVE", "NEGATIVE", "NEUTRAL"],
        },
        "category": {
            "type": "STRING",
        },
        "theme": {
            "type": "STRING",
        },
        "emotion": {
            "type": "STRING",
            "enum": ["Happy", "Frustrated", "Angry", "Concerned", "Neutral"],
        },
        "priority": {
            "type": "STRING",
            "enum": ["LOW", "MEDIUM", "HIGH", "CRITICAL"],
        },
        "department": {
            "type": "STRING",
        },
        "short_summary": {
            "type": "STRING",
        },
        "confidence_score": {
            "type": "NUMBER",
        },
    },
    "required": [
        "sentiment",
        "category",
        "theme",
        "emotion",
        "priority",
        "department",
        "short_summary",
        "confidence_score",
    ],
}

SYSTEM_INSTRUCTION = (
    "You are an expert customer feedback intelligence and decision-support engine for financial and banking operations. "
    "Your objective is to analyze customer comments and extract structured analytical metrics strictly following the provided schema. "
    "Required fields:\n"
    "- sentiment: must be exactly one of: POSITIVE, NEGATIVE, NEUTRAL.\n"
    "- category: functional banking category (e.g. Payments, Authentication, Account Services, Transfers, Mobile App, Customer Support, Cards).\n"
    "- theme: short business topic (e.g. OTP Issue, Duplicate Charge, Login Failure, Slow Transfer, App Crash).\n"
    "- emotion: must be exactly one of: Happy, Frustrated, Angry, Concerned, Neutral.\n"
    "- priority: must be exactly one of: LOW, MEDIUM, HIGH, CRITICAL.\n"
    "- department: resolving team (e.g. Payments Team, Digital Banking, Support Operations, Customer Service).\n"
    "- short_summary: exactly one sentence factual summary.\n"
    "- confidence_score: numeric value between 0.0 and 1.0.\n"
    "Output raw valid JSON only. Do not output markdown, code blocks, or conversational text."
)


class GeminiProvider:
    """
    Lightweight, direct Google AI Studio Gemini API Provider.
    Does not use LangChain, CrewAI, AutoGen, or LlamaIndex.
    Implements structured JSON output, exponential backoff retries, and strict Pydantic validation.
    """

    def __init__(
        self,
        api_key: Optional[str] = None,
        model_name: Optional[str] = None,
        timeout: Optional[float] = None,
        retries: Optional[int] = None,
    ):
        settings = get_settings()
        self._api_key = api_key or settings.GEMINI_API_KEY
        self.model_name = model_name or settings.GEMINI_TEXT_MODEL
        self.timeout = timeout or settings.AI_TIMEOUT_SECONDS
        self.retries = retries if retries is not None else settings.AI_RETRY_COUNT
        self.endpoint_url = (
            f"https://generativelanguage.googleapis.com/v1beta/models/{self.model_name}:generateContent"
        )

    def is_configured(self) -> bool:
        return bool(self._api_key and not self._api_key.startswith("your_"))

    def analyze_feedback(
        self,
        feedback_text: str,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> FeedbackAnalysisDTO:
        if not self.is_configured():
            raise ValueError("GEMINI_API_KEY is not configured or invalid.")

        prompt = self._build_prompt(feedback_text, metadata)
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": self._api_key,
        }
        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "systemInstruction": {"parts": [{"text": SYSTEM_INSTRUCTION}]},
            "generationConfig": {
                "response_mime_type": "application/json",
                "response_schema": GEMINI_ANALYSIS_SCHEMA,
                "temperature": 0.1,
                "maxOutputTokens": 1024,
            },
        }

        last_exception: Optional[Exception] = None

        candidate_models = [self.model_name]
        for fallback in ["gemini-3.1-flash-lite", "gemini-3-flash-preview"]:
            if fallback not in candidate_models:
                candidate_models.append(fallback)

        for current_model in candidate_models:
            endpoint_url = f"https://generativelanguage.googleapis.com/v1beta/models/{current_model}:generateContent"
            model_exhausted = False

            for attempt in range(1, self.retries + 1):
                try:
                    with httpx.Client(timeout=self.timeout) as client:
                        response = client.post(endpoint_url, headers=headers, json=payload)

                    if response.status_code == 429:
                        error_body = response.text
                        if "RESOURCE_EXHAUSTED" in error_body or "quota" in error_body.lower():
                            logger.warning(
                                "Model %s free tier quota exhausted. Attempting fallback if available.",
                                current_model,
                            )
                            model_exhausted = True
                            last_exception = httpx.HTTPStatusError(
                                f"Quota exhausted on {current_model}",
                                request=response.request,
                                response=response,
                            )
                            break
                        logger.warning("Gemini API rate limit (429) hit on %s attempt %d/%d", current_model, attempt, self.retries)
                    elif response.status_code == 404:
                        logger.warning("Model %s returned 404 Not Found.", current_model)
                        model_exhausted = True
                        last_exception = httpx.HTTPStatusError(
                            f"Model {current_model} not found",
                            request=response.request,
                            response=response,
                        )
                        break
                    elif response.status_code != 200:
                        logger.warning(
                            "Gemini API returned HTTP %s on attempt %d/%d for %s: %s",
                            response.status_code,
                            attempt,
                            self.retries,
                            current_model,
                            response.text[:200],
                        )

                    response.raise_for_status()

                    data = response.json()
                    candidates = data.get("candidates", [])
                    if not candidates:
                        raise ValueError(f"Gemini model {current_model} returned empty candidate list.")

                    raw_json_text = candidates[0]["content"]["parts"][0]["text"].strip()
                    # Clean any markdown codefence wrapping if present
                    if raw_json_text.startswith("```json"):
                        raw_json_text = raw_json_text[7:]
                    if raw_json_text.startswith("```"):
                        raw_json_text = raw_json_text[3:]
                    if raw_json_text.endswith("```"):
                        raw_json_text = raw_json_text[:-3]
                    raw_json_text = raw_json_text.strip()

                    parsed_json = json.loads(raw_json_text)
                    dto = FeedbackAnalysisDTO.model_validate(parsed_json)
                    dto.model_name = current_model
                    dto.model_version = current_model
                    return dto

                except (httpx.HTTPError, json.JSONDecodeError, ValidationError, ValueError, Exception) as ex:
                    last_exception = ex
                    logger.warning(
                        "Gemini analysis attempt %d/%d on %s failed: %s (%s).",
                        attempt,
                        self.retries,
                        current_model,
                        type(ex).__name__,
                        str(ex),
                    )
                    if model_exhausted:
                        break
                    if attempt < self.retries:
                        backoff = 0.5 * (2 ** (attempt - 1))
                        time.sleep(backoff)

            if not model_exhausted and last_exception and not isinstance(last_exception, httpx.HTTPStatusError):
                break

        raise RuntimeError(
            f"Gemini analysis failed after trying {candidate_models}. Cause: {last_exception}"
        ) from last_exception

    def _build_prompt(
        self,
        feedback_text: str,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> str:
        parts = [f'Customer Feedback: "{feedback_text.strip()}"']
        if metadata:
            if metadata.get("rating"):
                parts.append(f"Rating: {metadata['rating']}/5")
            if metadata.get("channel"):
                parts.append(f"Channel: {metadata['channel']}")
            if metadata.get("product"):
                parts.append(f"Product: {metadata['product']}")
            if metadata.get("customer_segment"):
                parts.append(f"Customer Segment: {metadata['customer_segment']}")
            if metadata.get("region"):
                parts.append(f"Region: {metadata['region']}")

        parts.append("Analyze this feedback and return the JSON object conforming to the schema.")
        return "\n".join(parts)
