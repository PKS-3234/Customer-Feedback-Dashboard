import logging
from typing import Dict, Any
from app.agents.analytics_agent import AnalyticsAgent
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


def analytics_node(state: AgentGraphState) -> Dict[str, Any]:
    """
    LangGraph node: Reuses existing AnalyticsAgent to aggregate sentiment,
    category, theme, and priority distributions.
    """
    query = state.get("query") or state.get("question") or ""
    limit = state.get("limit", 5)

    agent = AnalyticsAgent()
    evidence = agent.run(query=query, limit=limit)

    return {
        "analytics_results": evidence.get("data"),
        "supporting_evidence": [evidence],
        "agents_used": ["Analytics Agent"],
        "execution_path": ["analytics_node"],
    }
