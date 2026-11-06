import operator
from typing import TypedDict, Annotated, List, Dict, Any, Optional


class AgentGraphState(TypedDict, total=False):
    """
    State passed through the LangGraph StateGraph workflow.
    Uses operator.add reducers for list fields that aggregate outputs
    across fan-out branches (analytics, retrieval, quality).
    """
    query: str
    question: str
    limit: int
    selected_agents: List[str]
    analytics_results: Optional[Dict[str, Any]]
    retrieval_results: Optional[Dict[str, Any]]
    quality_results: Optional[Dict[str, Any]]
    supporting_evidence: Annotated[List[Dict[str, Any]], operator.add]
    agents_used: Annotated[List[str], operator.add]
    execution_path: Annotated[List[str], operator.add]
    final_response: Optional[str]
    answer: Optional[str]
