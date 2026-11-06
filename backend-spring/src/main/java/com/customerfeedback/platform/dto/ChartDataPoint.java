package com.customerfeedback.platform.dto;

public record ChartDataPoint(
        String label,
        long count,
        double percentage
) {}
