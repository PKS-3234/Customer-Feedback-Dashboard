from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient

from app.main import app
from app.graph.graph_builder import compile_feedback_graph, build_feedback_graph
from app.nodes.routing_node import determine_routes, routing_node
from app.nodes.analytics_node import analytics_node
from app.nodes.retrieval_node import retrieval_node
from app.nodes.quality_node import quality_node
from app.nodes.reporting_node import reporting_node
from app.workflow.langgraph_workflow import LangGraphFeedbackWorkflow

client = TestClient(app)


def test_graph_compilation():
    builder = build_feedback_graph()
    compiled = builder.compile()
    assert compiled is not None
    nodes = list(compiled.get_graph().nodes.keys())
    assert "routing_node" in nodes
    assert "analytics_node" in nodes
    assert "retrieval_node" in nodes
    assert "quality_node" in nodes
    assert "reporting_node" in nodes


def test_routing_node_determinations():
    assert determine_routes("What are customers unhappy about?") == ["analytics_node"]
    assert determine_routes("Find duplicate charge complaints") == ["retrieval_node"]
    assert determine_routes("What is embedding coverage?") == ["quality_node"]
    overview_routes = determine_routes("Give me an overview of customer sentiment")
    assert "analytics_node" in overview_routes
    assert "retrieval_node" in overview_routes
    assert "quality_node" in overview_routes


def test_routing_node_state_execution():
    state = {"query": "What are customers unhappy about?"}
    out = routing_node(state)
    assert out["selected_agents"] == ["analytics_node"]
    assert out["execution_path"] == ["routing_node"]


@patch("app.nodes.analytics_node.AnalyticsAgent")
def test_analytics_node_execution(mock_agent_cls):
    mock_instance = MagicMock()
    mock_instance.run.return_value = {
        "agent": "Analytics Agent",
        "type": "ANALYTICS_METRICS",
        "summary": "5 records analyzed",
        "data": {"total_analyzed": 5}
    }
    mock_agent_cls.return_value = mock_instance

    state = {"query": "customer sentiment", "limit": 5}
    out = analytics_node(state)

    assert out["analytics_results"] == {"total_analyzed": 5}
    assert len(out["supporting_evidence"]) == 1
    assert out["agents_used"] == ["Analytics Agent"]
    assert out["execution_path"] == ["analytics_node"]


@patch("app.nodes.retrieval_node.RetrievalAgent")
def test_retrieval_node_execution(mock_agent_cls):
    mock_instance = MagicMock()
    mock_instance.run.return_value = {
        "agent": "Retrieval Agent",
        "type": "RETRIEVAL_RESULTS",
        "summary": "Found 2 matching records",
        "data": {"matches": []}
    }
    mock_agent_cls.return_value = mock_instance

    state = {"query": "duplicate charges", "limit": 5}
    out = retrieval_node(state)

    assert "matches" in out["retrieval_results"]
    assert out["agents_used"] == ["Retrieval Agent"]
    assert out["execution_path"] == ["retrieval_node"]


@patch("app.nodes.quality_node.DataQualityAgent")
def test_quality_node_execution(mock_agent_cls):
    mock_instance = MagicMock()
    mock_instance.run.return_value = {
        "agent": "Data Quality Agent",
        "type": "DATA_QUALITY_METRICS",
        "summary": "100% embedding coverage",
        "data": {"embedding_coverage_percent": 100.0}
    }
    mock_agent_cls.return_value = mock_instance

    state = {"query": "embedding coverage", "limit": 5}
    out = quality_node(state)

    assert out["quality_results"]["embedding_coverage_percent"] == 100.0
    assert out["agents_used"] == ["Data Quality Agent"]
    assert out["execution_path"] == ["quality_node"]


@patch("app.nodes.reporting_node.ReportingAgent")
def test_reporting_node_execution(mock_agent_cls):
    mock_instance = MagicMock()
    mock_instance.generate_report.return_value = "Executive Summary: Grounded report."
    mock_agent_cls.return_value = mock_instance

    state = {
        "query": "What are customers unhappy about?",
        "supporting_evidence": [{"agent": "Analytics Agent", "summary": "Metrics"}],
        "agents_used": ["Analytics Agent"],
    }
    out = reporting_node(state)

    assert "Executive Summary" in out["answer"]
    assert out["agents_used"] == ["Reporting Agent"]
    assert out["execution_path"] == ["reporting_node"]


@patch("app.nodes.analytics_node.AnalyticsAgent")
@patch("app.nodes.retrieval_node.RetrievalAgent")
@patch("app.nodes.quality_node.DataQualityAgent")
@patch("app.nodes.reporting_node.ReportingAgent")
def test_langgraph_fanout_fanin_workflow(mock_reporting, mock_quality, mock_retrieval, mock_analytics):
    # Setup mock agents
    mock_analytics.return_value.run.return_value = {
        "agent": "Analytics Agent", "type": "ANALYTICS_METRICS", "summary": "Analytics done", "data": {}
    }
    mock_retrieval.return_value.run.return_value = {
        "agent": "Retrieval Agent", "type": "RETRIEVAL_RESULTS", "summary": "Retrieval done", "data": {}
    }
    mock_quality.return_value.run.return_value = {
        "agent": "Data Quality Agent", "type": "DATA_QUALITY_METRICS", "summary": "Quality done", "data": {}
    }
    mock_reporting.return_value.generate_report.return_value = "Comprehensive multi-agent executive summary."

    workflow = LangGraphFeedbackWorkflow()
    # Query matching overview triggers fan-out across all 3 nodes
    res = workflow.run("Give me an overview of customer sentiment", limit=5)

    assert "Comprehensive multi-agent executive summary" in res.answer
    assert "Analytics Agent" in res.agents_used
    assert "Retrieval Agent" in res.agents_used
    assert "Data Quality Agent" in res.agents_used
    assert "Reporting Agent" in res.agents_used

    # Verify execution path tracking: routing -> specialist nodes -> reporting
    assert res.execution_path[0] == "routing_node"
    assert "analytics_node" in res.execution_path
    assert "retrieval_node" in res.execution_path
    assert "quality_node" in res.execution_path
    assert res.execution_path[-1] == "reporting_node"


def test_internal_workflow_endpoint():
    resp = client.get("/internal/v1/agents/workflow")
    assert resp.status_code == 200
    data = resp.json()

    assert "nodes" in data
    assert "routing_node" in data["nodes"]
    assert "reporting_node" in data["nodes"]
    assert "edges" in data
    assert len(data["edges"]) >= 7
    assert "mermaid" in data
    assert "graph" in data["mermaid"]


def test_internal_workflow_endpoint_simulated_query():
    resp = client.get("/internal/v1/agents/workflow?query=What%20is%20embedding%20coverage%3F")
    assert resp.status_code == 200
    data = resp.json()

    assert data["simulated_query"] == "What is embedding coverage?"
    assert data["execution_path"] == ["routing_node", "quality_node", "reporting_node"]
