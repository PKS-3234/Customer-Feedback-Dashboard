package com.customerfeedback.platform.dto;

public record AgentStatusResponse(
        String name,
        String purpose,
        String availability,
        String executionHistory
) {}
