import json
import logging
from typing import List, Dict, Any, Optional
import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)

REPORTING_SYSTEM_INSTRUCTION = (
    "You are an Executive Customer Intelligence Reporting Agent for a banking and financial services enterprise. "
    "Your responsibility is to synthesize a professional, authoritative, and concise response to the user's inquiry, "
    "grounded strictly in the verifiable evidence collected by the specialized analytics, retrieval, and data quality agents.\n\n"
    "STRICT GROUNDING RULES:\n"
    "1. You must use ONLY the provided evidence. Never fabricate or hallucinate numbers, percentages, or quotes.\n"
    "2. If citing metrics, quote the exact figures from the evidence (e.g., sentiment breakdown, coverage percentages).\n"
    "3. When referencing customer complaints or feedback, quote or directly reference the retrieved feedback items.\n"
    "4. Structure your response clearly using GitHub markdown: Executive Summary, Key Insights, and Recommended Next Steps.\n"
    "5. Keep the tone executive, objective, and analytical."
)


class ReportingAgent:
    """
    Reporting Agent uses Google Gemini to synthesize evidence-backed, factual responses.
    Strictly forbids hallucinated statistics and references real evidence.
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

    def is_configured(self) -> bool:
        return bool(self._api_key and not self._api_key.startswith("your_"))

    def generate_report(
        self,
        query: str,
        evidence: List[Dict[str, Any]],
        agents_used: List[str],
    ) -> str:
        logger.info("ReportingAgent generating synthesis for query: '%s'", query)

        # Build evidence text block
        evidence_text = json.dumps(evidence, indent=2, default=str)

        prompt = (
            f"User Question: {query}\n\n"
            f"Agents Executed: {', '.join(agents_used)}\n\n"
            f"Verifiable Evidence Collected:\n"
            f"```json\n{evidence_text}\n```\n\n"
            "Please provide a comprehensive, evidence-grounded executive answer."
        )

        if not self.is_configured():
            return self._build_deterministic_fallback(query, evidence, agents_used)

        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": self._api_key,
        }
        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "systemInstruction": {"parts": [{"text": REPORTING_SYSTEM_INSTRUCTION}]},
            "generationConfig": {
                "temperature": 0.2,
                "maxOutputTokens": 2048,
            },
        }

        candidate_models = [self.model_name, "gemini-3.1-flash-lite", "gemini-3-flash-preview"]

        for current_model in candidate_models:
            endpoint_url = (
                f"https://generativelanguage.googleapis.com/v1beta/models/{current_model}:generateContent"
            )
            for attempt in range(1, self.retries + 1):
                try:
                    with httpx.Client(timeout=self.timeout) as client:
                        resp = client.post(endpoint_url, headers=headers, json=payload)

                    if resp.status_code == 429 and ("RESOURCE_EXHAUSTED" in resp.text or "quota" in resp.text.lower()):
                        logger.warning("Model %s quota exhausted during reporting; trying fallback.", current_model)
                        break

                    resp.raise_for_status()
                    data = resp.json()
                    candidates = data.get("candidates", [])
                    if candidates:
                        text = candidates[0]["content"]["parts"][0]["text"].strip()
                        return text

                except Exception as ex:
                    logger.warning("Reporting attempt %d on %s failed: %s", attempt, current_model, ex)

        # Deterministic fallback if Gemini is completely unavailable
        return self._build_deterministic_fallback(query, evidence, agents_used)

    def _build_deterministic_fallback(
        self,
        query: str,
        evidence: List[Dict[str, Any]],
        agents_used: List[str],
    ) -> str:
        """
        Grounded rule-based synthesis used when AI provider is offline or quota-limited.
        Ensures strict compliance with 'no fabricated statistics' rule.
        """
        lines = [
            f"### Executive Report: {query}\n",
            f"**Agents Used:** {', '.join(agents_used)}\n",
            "#### Summary of Verifiable Evidence:\n"
        ]

        for item in evidence:
            agent = item.get("agent", "Agent")
            summary = item.get("summary", "")
            data = item.get("data", {})
            lines.append(f"- **{agent}:** {summary}")

            if isinstance(data, list) and data:
                lines.append("  - **Key Records:**")
                for rec in data[:3]:
                    text = rec.get("feedback_text") or rec.get("text")
                    sim = rec.get("similarity")
                    sim_str = f" (Similarity: {sim:.1%})" if sim is not None else ""
                    lines.append(f"    - \"{text}\"{sim_str}")

            elif isinstance(data, dict):
                if "sentiment_distribution" in data:
                    lines.append(f"  - Sentiments: {data['sentiment_distribution']}")
                if "priority_distribution" in data:
                    lines.append(f"  - Priorities: {data['priority_distribution']}")
                if "analysis_coverage_percent" in data:
                    lines.append(
                        f"  - Coverage: Analysis {data.get('analysis_coverage_percent')}%, "
                        f"Embedding {data.get('embedding_coverage_percent')}%"
                    )

        lines.append("\n*Report synthesized from direct platform evidence records.*")
        return "\n".join(lines)
