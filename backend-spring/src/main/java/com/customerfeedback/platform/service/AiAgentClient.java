package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.AgentQueryResponse;
import com.customerfeedback.platform.dto.WorkflowInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AiAgentClient {

    private static final Logger log = LoggerFactory.getLogger(AiAgentClient.class);

    private final RestClient restClient;

    public AiAgentClient(@Value("${app.ai-service.base-url:http://localhost:8000}") String aiServiceBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public AgentQueryResponse executeAgentQuery(String query, int limit) {
        log.info("Dispatching LangGraph workflow query: '{}' (limit: {})", query, limit);
        Map<String, Object> payload = Map.of(
                "query", query,
                "limit", limit
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/internal/v1/agents/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("AI service returned empty agent response for query: " + query);
        }

        String answer = response.get("answer") != null ? response.get("answer").toString() : "";

        @SuppressWarnings("unchecked")
        List<String> agentsUsed = (List<String>) response.getOrDefault("agents_used", Collections.emptyList());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> supportingEvidence = (List<Map<String, Object>>) response.getOrDefault(
                "supporting_evidence",
                Collections.emptyList()
        );

        @SuppressWarnings("unchecked")
        List<String> executionPath = (List<String>) response.getOrDefault("execution_path", Collections.emptyList());

        return new AgentQueryResponse(answer, agentsUsed, supportingEvidence, executionPath);
    }

    public WorkflowInfoResponse getWorkflowInfo(String query) {
        log.info("Fetching LangGraph workflow info (query: {})", query);
        String uri = query != null && !query.isBlank()
                ? "/internal/v1/agents/workflow?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                : "/internal/v1/agents/workflow";

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("AI service returned empty workflow info");
        }

        @SuppressWarnings("unchecked")
        List<String> nodes = (List<String>) response.getOrDefault("nodes", Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> edges = (List<Map<String, String>>) response.getOrDefault("edges", Collections.emptyList());
        String mermaid = response.get("mermaid") != null ? response.get("mermaid").toString() : "";
        @SuppressWarnings("unchecked")
        List<String> executionPath = (List<String>) response.getOrDefault("execution_path", Collections.emptyList());
        String simulatedQuery = response.get("simulated_query") != null ? response.get("simulated_query").toString() : null;

        return new WorkflowInfoResponse(nodes, edges, mermaid, executionPath, simulatedQuery);
    }
}
