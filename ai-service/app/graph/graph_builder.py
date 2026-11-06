import logging
from typing import List
from langgraph.graph import StateGraph, START, END
from app.graph.graph_state import AgentGraphState
from app.nodes.routing_node import routing_node
from app.nodes.analytics_node import analytics_node
from app.nodes.retrieval_node import retrieval_node
from app.nodes.quality_node import quality_node
from app.nodes.reporting_node import reporting_node

logger = logging.getLogger(__name__)


def build_feedback_graph() -> StateGraph:
    """
    Constructs the LangGraph StateGraph with fan-out to selected specialist nodes
    and barrier synchronization fan-in to reporting_node.
    """
    builder = StateGraph(AgentGraphState)

    # 1. Add Nodes
    builder.add_node("routing_node", routing_node)
    builder.add_node("analytics_node", analytics_node)
    builder.add_node("retrieval_node", retrieval_node)
    builder.add_node("quality_node", quality_node)
    builder.add_node("reporting_node", reporting_node)

    # 2. Entry Edge
    builder.add_edge(START, "routing_node")

    # 3. Conditional Fan-Out from routing_node to 1 or more specialist nodes
    def route_condition(state: AgentGraphState) -> List[str]:
        return state.get("selected_agents", ["analytics_node"])

    builder.add_conditional_edges(
        "routing_node",
        route_condition,
        {
            "analytics_node": "analytics_node",
            "retrieval_node": "retrieval_node",
            "quality_node": "quality_node",
        }
    )

    # 4. Fan-In: All specialist nodes converge at reporting_node (barrier synchronization)
    builder.add_edge("analytics_node", "reporting_node")
    builder.add_edge("retrieval_node", "reporting_node")
    builder.add_edge("quality_node", "reporting_node")

    # 5. Exit Edge
    builder.add_edge("reporting_node", END)

    return builder


def compile_feedback_graph():
    """
    Compiles and returns executable LangGraph workflow.
    """
    builder = build_feedback_graph()
    compiled_app = builder.compile()
    logger.info("Compiled LangGraph Feedback StateGraph successfully")
    return compiled_app
