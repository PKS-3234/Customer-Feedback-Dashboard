package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.AgentQueryResponse;
import com.customerfeedback.platform.dto.WorkflowInfoResponse;
import com.customerfeedback.platform.service.AiAgentClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
class AgentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiAgentClient aiAgentClient;

    @Test
    void queryAgents_valid_returnsAnswerAndAgentsUsed() throws Exception {
        AgentQueryResponse mockResponse = new AgentQueryResponse(
                "Executive Summary: Customers report positive experience with transfers.",
                List.of("Analytics Agent", "Reporting Agent"),
                List.of(
                        Map.of(
                                "agent", "Analytics Agent",
                                "type", "ANALYTICS_METRICS",
                                "summary", "5 records analyzed"
                        )
                ),
                List.of("routing_node", "analytics_node", "reporting_node")
        );

        when(aiAgentClient.executeAgentQuery(eq("What are customers unhappy about?"), anyInt()))
                .thenReturn(mockResponse);

        String payload = """
            {
                "query": "What are customers unhappy about?",
                "limit": 5
            }
            """;

        mockMvc.perform(post("/api/v1/agents/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Executive Summary: Customers report positive experience with transfers."))
                .andExpect(jsonPath("$.agentsUsed", hasSize(2)))
                .andExpect(jsonPath("$.agentsUsed[0]").value("Analytics Agent"))
                .andExpect(jsonPath("$.agentsUsed[1]").value("Reporting Agent"))
                .andExpect(jsonPath("$.supportingEvidence", hasSize(1)))
                .andExpect(jsonPath("$.executionPath", hasSize(3)))
                .andExpect(jsonPath("$.executionPath[0]").value("routing_node"))
                .andExpect(jsonPath("$.executionPath[1]").value("analytics_node"))
                .andExpect(jsonPath("$.executionPath[2]").value("reporting_node"));
    }

    @Test
    void queryAgents_blankQuery_returnsBadRequest() throws Exception {
        String payload = """
            {
                "query": "",
                "limit": 5
            }
            """;

        mockMvc.perform(post("/api/v1/agents/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWorkflow_returnsTopologyAndMermaid() throws Exception {
        WorkflowInfoResponse mockWorkflow = new WorkflowInfoResponse(
                List.of("routing_node", "analytics_node", "retrieval_node", "quality_node", "reporting_node"),
                List.of(Map.of("source", "START", "target", "routing_node")),
                "graph TD;\n  routing_node --> analytics_node;",
                List.of("routing_node", "analytics_node", "reporting_node"),
                null
        );

        when(aiAgentClient.getWorkflowInfo(null)).thenReturn(mockWorkflow);

        mockMvc.perform(get("/api/v1/agents/workflow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes", hasSize(5)))
                .andExpect(jsonPath("$.nodes[0]", is("routing_node")))
                .andExpect(jsonPath("$.mermaid", is("graph TD;\n  routing_node --> analytics_node;")))
                .andExpect(jsonPath("$.executionPath", hasSize(3)));
    }

    @Test
    void getWorkflow_withQuery_returnsSimulatedPath() throws Exception {
        WorkflowInfoResponse mockWorkflow = new WorkflowInfoResponse(
                List.of("routing_node", "analytics_node", "retrieval_node", "quality_node", "reporting_node"),
                List.of(Map.of("source", "START", "target", "routing_node")),
                "graph TD;\n  routing_node --> quality_node;",
                List.of("routing_node", "quality_node", "reporting_node"),
                "What is embedding coverage?"
        );

        when(aiAgentClient.getWorkflowInfo("What is embedding coverage?")).thenReturn(mockWorkflow);

        mockMvc.perform(get("/api/v1/agents/workflow").param("query", "What is embedding coverage?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulatedQuery", is("What is embedding coverage?")))
                .andExpect(jsonPath("$.executionPath[1]", is("quality_node")));
    }
}
