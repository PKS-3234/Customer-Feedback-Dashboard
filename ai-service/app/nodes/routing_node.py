import logging
from typing import List, Dict, Any
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


def determine_routes(query: str) -> List[str]:
    """
    Deterministic rule-based intent routing logic.
    Supports single destinations or multi-destination fan-out.
    """
    q = query.lower().strip()

    # 0. Overview / comprehensive intent -> fan-out to all 3 specialist nodes
    if any(k in q for k in ["overview", "summary of everything", "all metrics", "full platform", "complete report"]):
        return ["analytics_node", "retrieval_node", "quality_node"]

    # 1. Retrieval intent keywords (specific complaints, search, feedback quotes, charges, payments)
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
        "stats", "statistics", "trend", "trends", "top", "satisfaction"
    ])

    routes: List[str] = []
    if is_analytics and not (is_retrieval and not any(k in q for k in ["sentiment", "distribution", "breakdown", "metrics", "stats"])):
        routes.append("analytics_node")
    if is_retrieval:
        routes.append("retrieval_node")
    if is_quality:
        routes.append("quality_node")

    if not routes:
        routes = ["analytics_node", "retrieval_node"]

    return routes


def routing_node(state: AgentGraphState) -> Dict[str, Any]:
    """
    LangGraph node: Evaluates incoming query and selects downstream specialist nodes.
    """
    query = state.get("query") or state.get("question") or ""
    selected = determine_routes(query)
    logger.info("routing_node dispatched query '%s' to nodes: %s", query, selected)

    return {
        "selected_agents": selected,
        "execution_path": ["routing_node"],
    }
