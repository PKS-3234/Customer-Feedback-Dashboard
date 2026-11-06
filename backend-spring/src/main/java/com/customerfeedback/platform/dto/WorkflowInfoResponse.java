package com.customerfeedback.platform.dto;

import java.util.List;
import java.util.Map;

public record WorkflowInfoResponse(
        List<String> nodes,
        List<Map<String, String>> edges,
        String mermaid,
        List<String> executionPath,
        String simulatedQuery
) {}
