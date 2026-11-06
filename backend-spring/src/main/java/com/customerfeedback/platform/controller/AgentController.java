package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.AgentQueryRequest;
import com.customerfeedback.platform.dto.AgentQueryResponse;
import com.customerfeedback.platform.dto.WorkflowInfoResponse;
import com.customerfeedback.platform.service.AiAgentClient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AiAgentClient aiAgentClient;

    public AgentController(AiAgentClient aiAgentClient) {
        this.aiAgentClient = aiAgentClient;
    }

    @PostMapping("/query")
    public ResponseEntity<AgentQueryResponse> queryAgents(
            @Valid @RequestBody AgentQueryRequest request) {
        AgentQueryResponse response = aiAgentClient.executeAgentQuery(request.query(), request.limit());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflow")
    public ResponseEntity<WorkflowInfoResponse> getWorkflow(
            @RequestParam(name = "query", required = false) String query) {
        WorkflowInfoResponse response = aiAgentClient.getWorkflowInfo(query);
        return ResponseEntity.ok(response);
    }
}
