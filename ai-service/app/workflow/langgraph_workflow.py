import logging
from typing import Optional, Dict, Any, List
from app.graph.graph_builder import compile_feedback_graph
from app.graph.graph_state import AgentGraphState
from app.models.agent import AgentQueryResponse, WorkflowInfoResponse, WorkflowEdge
from app.nodes.routing_node import determine_routes

logger = logging.getLogger(__name__)


class LangGraphFeedbackWorkflow:
    """
    LangGraph StateGraph workflow orchestrator.
    Replaces supervisor routing with graph-based fan-out / fan-in execution.
    """

    def __init__(self):
        self.app = compile_feedback_graph()

    def run(self, query: str, limit: int = 5) -> AgentQueryResponse:
        logger.info("Executing LangGraph StateGraph workflow for query: '%s'", query)
        initial_state: AgentGraphState = {
            "query": query,
            "question": query,
            "limit": limit,
            "selected_agents": [],
            "supporting_evidence": [],
            "agents_used": [],
            "execution_path": [],
        }

        result = self.app.invoke(initial_state)

        answer = result.get("answer") or result.get("final_response") or "No response generated."
        # Remove duplicate agent names while preserving order
        raw_agents = result.get("agents_used", [])
        agents_used = list(dict.fromkeys(raw_agents))
        if "Reporting Agent" not in agents_used:
            agents_used.append("Reporting Agent")

        supporting_evidence = result.get("supporting_evidence", [])
        execution_path = result.get("execution_path", [])

        return AgentQueryResponse(
            answer=answer,
            agents_used=agents_used,
            supporting_evidence=supporting_evidence,
            execution_path=execution_path,
        )

    def get_workflow_info(self, query: Optional[str] = None) -> WorkflowInfoResponse:
        """
        Exposes workflow graph topology and simulates execution path if query is supplied.
        """
        nodes = ["routing_node", "analytics_node", "retrieval_node", "quality_node", "reporting_node"]
        edges = [
            WorkflowEdge(source="START", target="routing_node"),
            WorkflowEdge(source="routing_node", target="analytics_node"),
            WorkflowEdge(source="routing_node", target="retrieval_node"),
            WorkflowEdge(source="routing_node", target="quality_node"),
            WorkflowEdge(source="analytics_node", target="reporting_node"),
            WorkflowEdge(source="retrieval_node", target="reporting_node"),
            WorkflowEdge(source="quality_node", target="reporting_node"),
            WorkflowEdge(source="reporting_node", target="END"),
        ]

        mermaid_str = self.app.get_graph().draw_mermaid()

        simulated_path: Optional[List[str]] = None
        if query:
            selected_nodes = determine_routes(query)
            simulated_path = ["routing_node"] + selected_nodes + ["reporting_node"]

        return WorkflowInfoResponse(
            nodes=nodes,
            edges=edges,
            mermaid=mermaid_str,
            execution_path=simulated_path or ["routing_node", "analytics_node", "reporting_node"],
            simulated_query=query,
        )
