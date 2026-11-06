from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field


class AgentQueryRequest(BaseModel):
    query: str = Field(min_length=1, description="Natural language question for the multi-agent copilot")
    limit: Optional[int] = Field(default=5, ge=1, le=50, description="Maximum number of evidence records to retrieve")


class AgentQueryResponse(BaseModel):
    answer: str
    agents_used: List[str]
    supporting_evidence: List[Dict[str, Any]]
    execution_path: Optional[List[str]] = None


class WorkflowEdge(BaseModel):
    source: str
    target: str


class WorkflowInfoResponse(BaseModel):
    nodes: List[str]
    edges: List[WorkflowEdge]
    mermaid: str
    execution_path: Optional[List[str]] = None
    simulated_query: Optional[str] = None
