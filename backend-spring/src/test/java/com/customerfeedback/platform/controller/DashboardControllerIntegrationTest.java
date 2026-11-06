package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.*;
import com.customerfeedback.platform.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void getDashboardMetrics_returnsCompleteDashboardPayload() throws Exception {
        DashboardKeyMetrics keyMetrics = new DashboardKeyMetrics(
                10L, 8L, 8L, 62.5, 37.5, 2L, 2L, 1L, 1L, 0L
        );

        DashboardCharts charts = new DashboardCharts(
                List.of(new ChartDataPoint("POSITIVE", 5, 62.5), new ChartDataPoint("NEGATIVE", 3, 37.5)),
                List.of(new ChartDataPoint("Mobile App", 4, 50.0), new ChartDataPoint("Account Services", 4, 50.0)),
                List.of(new ChartDataPoint("Login Experience", 3, 37.5)),
                List.of(new ChartDataPoint("HIGH", 2, 25.0), new ChartDataPoint("LOW", 6, 75.0)),
                List.of(new ChartDataPoint("MOBILE_APP", 6, 60.0), new ChartDataPoint("BRANCH", 4, 40.0)),
                List.of(new ChartDataPoint("Checking Account", 7, 70.0), new ChartDataPoint("Mortgage", 3, 30.0)),
                List.of(new ChartDataPoint("RATING_INVALID", 1, 100.0))
        );

        SemanticSearchStatusResponse semanticSearchStatus = new SemanticSearchStatusResponse(
                "gemini-embedding-001",
                768,
                8L,
                "OPERATIONAL",
                "ENABLED (HNSW Index Active)"
        );

        List<String> exampleQuestions = List.of(
                "What are customers unhappy about?",
                "What are top themes?",
                "Show duplicate charge complaints.",
                "What is embedding coverage?"
        );

        DashboardResponse mockResponse = new DashboardResponse(
                keyMetrics,
                charts,
                semanticSearchStatus,
                exampleQuestions
        );

        when(dashboardService.getDashboardMetrics()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyMetrics.totalFeedbackRecords", is(10)))
                .andExpect(jsonPath("$.keyMetrics.totalAnalyzedRecords", is(8)))
                .andExpect(jsonPath("$.keyMetrics.totalEmbeddedRecords", is(8)))
                .andExpect(jsonPath("$.keyMetrics.positiveFeedbackPercent", is(62.5)))
                .andExpect(jsonPath("$.keyMetrics.negativeFeedbackPercent", is(37.5)))
                .andExpect(jsonPath("$.keyMetrics.highPriorityFeedbackCount", is(2)))
                .andExpect(jsonPath("$.keyMetrics.currentBatches", is(2)))
                .andExpect(jsonPath("$.keyMetrics.processingErrors", is(1)))
                .andExpect(jsonPath("$.keyMetrics.failedRows", is(1)))
                .andExpect(jsonPath("$.keyMetrics.duplicateRows", is(0)))
                .andExpect(jsonPath("$.charts.sentimentDistribution", hasSize(2)))
                .andExpect(jsonPath("$.charts.categoryDistribution", hasSize(2)))
                .andExpect(jsonPath("$.charts.themeDistribution", hasSize(1)))
                .andExpect(jsonPath("$.charts.priorityDistribution", hasSize(2)))
                .andExpect(jsonPath("$.charts.feedbackByChannel", hasSize(2)))
                .andExpect(jsonPath("$.charts.feedbackByProduct", hasSize(2)))
                .andExpect(jsonPath("$.charts.errorDistribution", hasSize(1)))
                .andExpect(jsonPath("$.semanticSearchStatus.embeddingModel", is("gemini-embedding-001")))
                .andExpect(jsonPath("$.semanticSearchStatus.embeddingDimension", is(768)))
                .andExpect(jsonPath("$.semanticSearchStatus.totalEmbeddings", is(8)))
                .andExpect(jsonPath("$.semanticSearchStatus.semanticSearchStatus", is("OPERATIONAL")))
                .andExpect(jsonPath("$.semanticSearchStatus.pgvectorStatus", is("ENABLED (HNSW Index Active)")))
                .andExpect(jsonPath("$.exampleQuestions", hasSize(4)));
    }

    @Test
    void getAgents_returnsConfiguredAgentsList() throws Exception {
        List<AgentStatusResponse> agents = List.of(
                new AgentStatusResponse("Supervisor Agent", "Deterministic intent routing", "AVAILABLE", "Not Tracked"),
                new AgentStatusResponse("Analytics Agent", "Aggregates feedback distributions", "AVAILABLE", "Not Tracked"),
                new AgentStatusResponse("Retrieval Agent", "Semantic search over PGVector", "AVAILABLE", "Not Tracked"),
                new AgentStatusResponse("Data Quality Agent", "Monitors ingestion and errors", "AVAILABLE", "Not Tracked"),
                new AgentStatusResponse("Reporting Agent", "Evidence-grounded summaries", "AVAILABLE", "Not Tracked")
        );

        when(dashboardService.getAgents()).thenReturn(agents);

        mockMvc.perform(get("/api/v1/dashboard/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].name", is("Supervisor Agent")))
                .andExpect(jsonPath("$[0].availability", is("AVAILABLE")))
                .andExpect(jsonPath("$[0].executionHistory", is("Not Tracked")))
                .andExpect(jsonPath("$[4].name", is("Reporting Agent")));
    }
}
