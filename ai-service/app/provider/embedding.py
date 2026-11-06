import logging
import time
from typing import List, Optional
import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class EmbeddingProvider:
    """
    Lightweight, direct Google AI Studio Gemini Embedding Provider.
    Invokes models/gemini-embedding-001 directly via HTTP without third-party agent frameworks.
    Supports outputDimensionality (default 768) and exponential backoff retry.
    """

    def __init__(
        self,
        api_key: Optional[str] = None,
        model_name: Optional[str] = None,
        default_dimension: Optional[int] = None,
        timeout: Optional[float] = None,
        retries: Optional[int] = None,
    ):
        settings = get_settings()
        self._api_key = api_key or settings.GEMINI_API_KEY
        self.model_name = model_name or settings.GEMINI_EMBEDDING_MODEL
        self.default_dimension = default_dimension or settings.GEMINI_EMBEDDING_DIMENSION
        self.timeout = timeout or settings.AI_TIMEOUT_SECONDS
        self.retries = retries if retries is not None else settings.AI_RETRY_COUNT
        self.endpoint_url = (
            f"https://generativelanguage.googleapis.com/v1beta/models/{self.model_name}:embedContent"
        )

    def is_configured(self) -> bool:
        return bool(self._api_key and not self._api_key.startswith("your_"))

    def generate_embedding(
        self,
        text: str,
        dimension: Optional[int] = None,
        task_type: Optional[str] = "RETRIEVAL_DOCUMENT",
    ) -> List[float]:
        """
        Generate vector embedding for text using Google AI Studio gemini-embedding-001.
        """
        if not self.is_configured():
            raise ValueError("GEMINI_API_KEY is not configured or invalid.")

        cleaned_text = text.strip()
        if not cleaned_text:
            raise ValueError("Input text for embedding cannot be blank.")

        target_dimension = dimension or self.default_dimension

        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": self._api_key,
        }

        payload = {
            "content": {"parts": [{"text": cleaned_text}]},
            "outputDimensionality": target_dimension,
        }

        last_exception: Optional[Exception] = None

        for attempt in range(1, self.retries + 1):
            try:
                with httpx.Client(timeout=self.timeout) as client:
                    response = client.post(self.endpoint_url, headers=headers, json=payload)

                if response.status_code == 429:
                    logger.warning("Gemini Embedding API 429 rate limit hit on attempt %d/%d", attempt, self.retries)
                elif response.status_code != 200:
                    logger.warning(
                        "Gemini Embedding API returned HTTP %s on attempt %d/%d: %s",
                        response.status_code,
                        attempt,
                        self.retries,
                        response.text[:200],
                    )

                response.raise_for_status()

                data = response.json()
                embedding_data = data.get("embedding", {})
                values = embedding_data.get("values", [])

                if not values:
                    raise ValueError("Gemini returned empty embedding values vector.")

                if len(values) != target_dimension:
                    logger.warning(
                        "Returned embedding dimension %d differs from requested %d. Truncating or padding.",
                        len(values),
                        target_dimension,
                    )
                    if len(values) > target_dimension:
                        values = values[:target_dimension]

                return values

            except (httpx.HTTPError, ValueError, Exception) as ex:
                last_exception = ex
                logger.warning(
                    "Embedding generation attempt %d/%d failed: %s (%s).",
                    attempt,
                    self.retries,
                    type(ex).__name__,
                    str(ex),
                )
                if attempt < self.retries:
                    backoff = 0.5 * (2 ** (attempt - 1))
                    time.sleep(backoff)

        raise RuntimeError(
            f"Gemini embedding generation failed after {self.retries} attempts. Cause: {last_exception}"
        ) from last_exception
