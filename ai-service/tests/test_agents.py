from unittest.mock import MagicMock
from fastapi.testclient import TestClient

from app.main import app
from app.agents.supervisor_agent import SupervisorAgent
from app.agents.analytics_agent import AnalyticsAgent
from app.agents.retrieval_agent import RetrievalAgent
from app.agents.data_quality_agent import DataQualityAgent
from app.agents.reporting_agent import ReportingAgent

client = TestClient(app)


def test_supervisor_agent_routing_analytics():
    supervisor = SupervisorAgent()
    routes = supervisor.route_query("What are customers unhappy about?")
    assert "Analytics Agent" in routes
    assert "Data Quality Agent" not in routes


def test_supervisor_agent_routing_retrieval():
    supervisor = SupervisorAgent()
    routes = supervisor.route_query("Show complaints about duplicate charges")
    assert "Retrieval Agent" in routes
    assert "Data Quality Agent" not in routes


def test_supervisor_agent_routing_data_quality():
    supervisor = SupervisorAgent()
    routes = supervisor.route_query("What is embedding coverage?")
    assert "Data Quality Agent" in routes
    assert "Retrieval Agent" not in routes


def test_supervisor_agent_run_orchestration():
    mock_analytics = MagicMock(spec=AnalyticsAgent)
    mock_analytics.run.return_value = {
        "agent": "Analytics Agent",
        "type": "ANALYTICS_METRICS",
        "summary": "Analyzed 10 records",
        "data": {"total_analyzed": 10, "sentiment_distribution": {"POSITIVE": 7, "NEGATIVE": 3}}
    }

    mock_reporting = MagicMock(spec=ReportingAgent)
    mock_reporting.generate_report.return_value = "Executive summary: 70% positive sentiment."

    supervisor = SupervisorAgent(
        analytics_agent=mock_analytics,
        reporting_agent=mock_reporting
    )

    response = supervisor.run("What is customer sentiment?")
    assert "Analytics Agent" in response.agents_used
    assert "Reporting Agent" in response.agents_used
    assert "Executive summary" in response.answer
    assert len(response.supporting_evidence) >= 1


def test_reporting_agent_deterministic_fallback():
    reporting = ReportingAgent(api_key="your_unconfigured_key")
    evidence = [
        {
            "agent": "Analytics Agent",
            "summary": "Analyzed 5 records",
            "data": {"sentiment_distribution": {"POSITIVE": 3, "NEGATIVE": 2}}
        }
    ]
    report = reporting.generate_report("What is sentiment?", evidence, ["Analytics Agent", "Reporting Agent"])
    assert "Executive Report" in report
    assert "Analytics Agent" in report
    assert "POSITIVE" in report


def test_internal_agent_query_endpoint():
    dummy_response = {
        "answer": "Grounded answer from agents.",
        "agents_used": ["Analytics Agent", "Reporting Agent"],
        "supporting_evidence": [
            {"agent": "Analytics Agent", "type": "TEST", "summary": "test", "data": {}}
        ]
    }

    with client:
        # Patch the supervisor_agent.run
        from app.api.v1.endpoints import agent
        orig_run = agent.supervisor_agent.run
        try:
            agent.supervisor_agent.run = MagicMock(return_value=dummy_response)
            response = client.post(
                "/internal/v1/agents/query",
                json={"query": "What are customers unhappy about?", "limit": 5}
            )
            assert response.status_code == 200
            data = response.json()
            assert data["answer"] == "Grounded answer from agents."
            assert "Analytics Agent" in data["agents_used"]
            assert len(data["supporting_evidence"]) == 1
        finally:
            agent.supervisor_agent.run = orig_run
