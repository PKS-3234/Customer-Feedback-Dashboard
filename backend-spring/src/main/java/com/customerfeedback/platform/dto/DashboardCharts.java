package com.customerfeedback.platform.dto;

import java.util.List;

public record DashboardCharts(
        List<ChartDataPoint> sentimentDistribution,
        List<ChartDataPoint> categoryDistribution,
        List<ChartDataPoint> themeDistribution,
        List<ChartDataPoint> priorityDistribution,
        List<ChartDataPoint> feedbackByChannel,
        List<ChartDataPoint> feedbackByProduct,
        List<ChartDataPoint> errorDistribution
) {}
