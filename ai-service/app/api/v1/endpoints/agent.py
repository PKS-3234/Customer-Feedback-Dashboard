import logging
from typing import Optional
from fastapi import APIRouter, HTTPException, Query, status

from app.models.agent import AgentQueryRequest, AgentQueryResponse, WorkflowInfoResponse
from app.workflow.langgraph_workflow import LangGraphFeedbackWorkflow

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/internal/v1/agents", tags=["internal-agents"])
langgraph_workflow = LangGraphFeedbackWorkflow()
supervisor_agent = langgraph_workflow  # Backward-compatible alias for existing tests


@router.post(
    "/query",
    response_model=AgentQueryResponse,
    status_code=status.HTTP_200_OK,
    summary="Execute multi-agent query workflow with LangGraph StateGraph orchestration",
)
def execute_agent_query(request: AgentQueryRequest) -> AgentQueryResponse:
    try:
        return langgraph_workflow.run(
            query=request.query,
            limit=request.limit or 5,
        )
    except Exception as e:
        logger.error("LangGraph execution failed for query '%s': %s", request.query, e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"LangGraph workflow execution failed: {str(e)}",
        )


@router.get(
    "/workflow",
    response_model=WorkflowInfoResponse,
    status_code=status.HTTP_200_OK,
    summary="Retrieve LangGraph workflow topology, nodes, edges, mermaid diagram, and simulated execution path",
)
def get_workflow_info(
    query: Optional[str] = Query(default=None, description="Optional query to simulate routing and execution path")
) -> WorkflowInfoResponse:
    try:
        return langgraph_workflow.get_workflow_info(query=query)
    except Exception as e:
        logger.error("Failed to retrieve workflow info: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to retrieve LangGraph workflow information: {str(e)}",
        )
