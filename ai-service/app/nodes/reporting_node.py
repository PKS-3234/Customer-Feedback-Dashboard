import logging
from typing import Dict, Any
from app.agents.reporting_agent import ReportingAgent
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


def reporting_node(state: AgentGraphState) -> Dict[str, Any]:
    """
    LangGraph node: Reuses existing ReportingAgent to synthesize an executive summary
    and recommendations grounded strictly on actual supporting evidence gathered.
    """
    query = state.get("query") or state.get("question") or ""
    evidence = state.get("supporting_evidence", [])
    current_agents = state.get("agents_used", [])

    all_agents = list(dict.fromkeys(current_agents + ["Reporting Agent"]))

    agent = ReportingAgent()
    report = agent.generate_report(
        query=query,
        evidence=evidence,
        agents_used=all_agents
    )

    return {
        "answer": report,
        "final_response": report,
        "agents_used": ["Reporting Agent"],
        "execution_path": ["reporting_node"],
    }
