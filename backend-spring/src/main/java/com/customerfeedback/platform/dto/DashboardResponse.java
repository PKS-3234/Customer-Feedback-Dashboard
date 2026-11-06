package com.customerfeedback.platform.dto;

import java.util.List;

public record DashboardResponse(
        DashboardKeyMetrics keyMetrics,
        DashboardCharts charts,
        SemanticSearchStatusResponse semanticSearchStatus,
        List<String> exampleQuestions
) {}
