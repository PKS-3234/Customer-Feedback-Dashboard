import logging
from typing import Dict, Any
from app.agents.data_quality_agent import DataQualityAgent
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


def quality_node(state: AgentGraphState) -> Dict[str, Any]:
    """
    LangGraph node: Reuses existing DataQualityAgent to monitor ingestion batches,
    validation errors, and coverage metrics.
    """
    query = state.get("query") or state.get("question") or ""
    limit = state.get("limit", 5)

    agent = DataQualityAgent()
    evidence = agent.run(query=query, limit=limit)

    return {
        "quality_results": evidence.get("data"),
        "supporting_evidence": [evidence],
        "agents_used": ["Data Quality Agent"],
        "execution_path": ["quality_node"],
    }
