package com.customerfeedback.platform.dto;

import java.util.List;
import java.util.Map;

public record AgentQueryResponse(
        String answer,
        List<String> agentsUsed,
        List<Map<String, Object>> supportingEvidence,
        List<String> executionPath
) {
    public AgentQueryResponse(String answer, List<String> agentsUsed, List<Map<String, Object>> supportingEvidence) {
        this(answer, agentsUsed, supportingEvidence, List.of());
    }
}
