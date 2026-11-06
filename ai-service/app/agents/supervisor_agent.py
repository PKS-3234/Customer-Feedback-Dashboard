import logging
from typing import List, Dict, Any, Optional

from app.agents.analytics_agent import AnalyticsAgent
from app.agents.retrieval_agent import RetrievalAgent
from app.agents.data_quality_agent import DataQualityAgent
from app.agents.reporting_agent import ReportingAgent
from app.models.agent import AgentQueryResponse

logger = logging.getLogger(__name__)


class SupervisorAgent:
    """
    Supervisor Agent uses deterministic routing logic to decide which analytical,
    retrieval, or quality agents are invoked for a given user query.
    Always terminates with the Reporting Agent for evidence synthesis.
    """

    def __init__(
        self,
        analytics_agent: Optional[AnalyticsAgent] = None,
        retrieval_agent: Optional[RetrievalAgent] = None,
        data_quality_agent: Optional[DataQualityAgent] = None,
        reporting_agent: Optional[ReportingAgent] = None,
    ):
        self.analytics_agent = analytics_agent or AnalyticsAgent()
        self.retrieval_agent = retrieval_agent or RetrievalAgent()
        self.data_quality_agent = data_quality_agent or DataQualityAgent()
        self.reporting_agent = reporting_agent or ReportingAgent()

    def route_query(self, query: str) -> List[str]:
        """
        Purely deterministic rule-based routing based on query intent keywords.
        """
        q = query.lower()

        # 1. Retrieval intent keywords (specific feedback, complaints, search, examples)
        is_retrieval = any(k in q for k in [
            "show", "complaint", "complaints", "examples", "search",
            "find", "feedback", "quote", "quotes", "specific", "representative",
            "advisor", "delay", "charges", "charge", "payment", "payments",
            "login", "transfer", "crash"
        ])

        # 2. Data quality intent keywords (system metrics, coverage, errors, batch ingestion)
        is_quality = any(k in q for k in [
            "coverage", "quality", "error", "errors", "fail", "failed", "failure",
            "failures", "ingestion", "batch", "batches", "health", "pipeline",
            "duplicate row", "duplicate rows", "duplicate count", "duplicate record"
        ]) or ("duplicate" in q and not is_retrieval)

        # 3. Analytics aggregation intent keywords (sentiment, distribution, themes, priorities)
        is_analytics = any(k in q for k in [
            "unhappy", "happy", "sentiment", "distribution", "category", "categories",
            "theme", "themes", "priority", "priorities", "breakdown", "metrics",
            "stats", "statistics", "trend", "trends", "top", "satisfaction", "overall"
        ])

        selected = []
        if is_analytics and not (is_retrieval and not any(k in q for k in ["sentiment", "distribution", "breakdown", "metrics", "stats"])):
            selected.append("Analytics Agent")
        if is_retrieval:
            selected.append("Retrieval Agent")
        if is_quality:
            selected.append("Data Quality Agent")

        # If nothing matched explicitly, use analytics + retrieval as safe default
        if not selected:
            selected = ["Analytics Agent", "Retrieval Agent"]

        return selected

    def run(self, query: str, limit: int = 5) -> AgentQueryResponse:
        logger.info("SupervisorAgent processing query: '%s'", query)
        selected_agent_names = self.route_query(query)

        supporting_evidence: List[Dict[str, Any]] = []

        # Execute chosen agents
        if "Analytics Agent" in selected_agent_names:
            evidence = self.analytics_agent.run(query, limit=limit)
            supporting_evidence.append(evidence)

        if "Retrieval Agent" in selected_agent_names:
            evidence = self.retrieval_agent.run(query, limit=limit)
            supporting_evidence.append(evidence)

        if "Data Quality Agent" in selected_agent_names:
            evidence = self.data_quality_agent.run(query, limit=limit)
            supporting_evidence.append(evidence)

        # Reporting Agent is always used for final answer synthesis
        all_agents_used = selected_agent_names + ["Reporting Agent"]

        answer = self.reporting_agent.generate_report(
            query=query,
            evidence=supporting_evidence,
            agents_used=all_agents_used
        )

        return AgentQueryResponse(
            answer=answer,
            agents_used=all_agents_used,
            supporting_evidence=supporting_evidence
        )
