import logging
from typing import Dict, Any
from app.agents.retrieval_agent import RetrievalAgent
from app.graph.graph_state import AgentGraphState

logger = logging.getLogger(__name__)


def retrieval_node(state: AgentGraphState) -> Dict[str, Any]:
    """
    LangGraph node: Reuses existing RetrievalAgent to perform PGVector cosine
    semantic search over feedback embeddings.
    """
    query = state.get("query") or state.get("question") or ""
    limit = state.get("limit", 5)

    agent = RetrievalAgent()
    evidence = agent.run(query=query, limit=limit)

    return {
        "retrieval_results": evidence.get("data"),
        "supporting_evidence": [evidence],
        "agents_used": ["Retrieval Agent"],
        "execution_path": ["retrieval_node"],
    }
