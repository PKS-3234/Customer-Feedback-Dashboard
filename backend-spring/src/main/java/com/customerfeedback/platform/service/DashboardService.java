package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.*;
import com.customerfeedback.platform.entity.FeedbackEmbedding;
import com.customerfeedback.platform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final FeedbackRepository feedbackRepository;
    private final FeedbackAnalysisRepository feedbackAnalysisRepository;
    private final FeedbackEmbeddingRepository feedbackEmbeddingRepository;
    private final IngestionBatchRepository ingestionBatchRepository;
    private final ProcessingErrorRepository processingErrorRepository;

    private static final List<String> EXAMPLE_QUESTIONS = List.of(
            "What are customers unhappy about?",
            "What are top themes?",
            "Show duplicate charge complaints.",
            "What is embedding coverage?"
    );

    public DashboardService(
            FeedbackRepository feedbackRepository,
            FeedbackAnalysisRepository feedbackAnalysisRepository,
            FeedbackEmbeddingRepository feedbackEmbeddingRepository,
            IngestionBatchRepository ingestionBatchRepository,
            ProcessingErrorRepository processingErrorRepository
    ) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackAnalysisRepository = feedbackAnalysisRepository;
        this.feedbackEmbeddingRepository = feedbackEmbeddingRepository;
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.processingErrorRepository = processingErrorRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardMetrics() {
        log.info("Calculating executive dashboard metrics");

        long totalFeedback = feedbackRepository.count();
        long totalAnalyzed = feedbackAnalysisRepository.countByIsCurrentTrue();
        long totalEmbedded = feedbackEmbeddingRepository.countByIsCurrentTrue();

        long positiveCount = feedbackAnalysisRepository.countByIsCurrentTrueAndSentimentIgnoreCase("POSITIVE");
        long negativeCount = feedbackAnalysisRepository.countByIsCurrentTrueAndSentimentIgnoreCase("NEGATIVE");
        long highPriorityCount = feedbackAnalysisRepository.countByIsCurrentTrueAndPriorityIgnoreCase("HIGH");

        double positivePercent = totalAnalyzed > 0
                ? Math.round((positiveCount * 1000.0) / totalAnalyzed) / 10.0
                : 0.0;
        double negativePercent = totalAnalyzed > 0
                ? Math.round((negativeCount * 1000.0) / totalAnalyzed) / 10.0
                : 0.0;

        long currentBatches = ingestionBatchRepository.count();
        long processingErrors = processingErrorRepository.count();
        long failedRows = ingestionBatchRepository.sumFailedRows();
        long duplicateRows = ingestionBatchRepository.sumDuplicateRows();

        DashboardKeyMetrics keyMetrics = new DashboardKeyMetrics(
                totalFeedback,
                totalAnalyzed,
                totalEmbedded,
                positivePercent,
                negativePercent,
                highPriorityCount,
                currentBatches,
                processingErrors,
                failedRows,
                duplicateRows
        );

        // Charts
        List<ChartDataPoint> sentimentChart = toChartData(feedbackAnalysisRepository.findSentimentDistribution(), totalAnalyzed);
        List<ChartDataPoint> categoryChart = toChartData(feedbackAnalysisRepository.findCategoryDistribution(), totalAnalyzed);
        List<ChartDataPoint> themeChart = toChartData(feedbackAnalysisRepository.findThemeDistribution(), totalAnalyzed);
        List<ChartDataPoint> priorityChart = toChartData(feedbackAnalysisRepository.findPriorityDistribution(), totalAnalyzed);
        List<ChartDataPoint> channelChart = toChartData(feedbackRepository.findChannelDistribution(), totalFeedback);
        List<ChartDataPoint> productChart = toChartData(feedbackRepository.findProductDistribution(), totalFeedback);
        List<ChartDataPoint> errorChart = toChartData(processingErrorRepository.findErrorDistribution(), processingErrors);

        DashboardCharts charts = new DashboardCharts(
                sentimentChart,
                categoryChart,
                themeChart,
                priorityChart,
                channelChart,
                productChart,
                errorChart
        );

        // Semantic Search Status
        Optional<FeedbackEmbedding> latestEmbedding = feedbackEmbeddingRepository.findFirstByIsCurrentTrueOrderByCreatedAtDesc();
        String embeddingModel = latestEmbedding.map(FeedbackEmbedding::getEmbeddingModel).orElse("gemini-embedding-001");
        int embeddingDimension = latestEmbedding.map(FeedbackEmbedding::getEmbeddingDimension).orElse(768);
        String searchStatus = totalEmbedded > 0 ? "OPERATIONAL" : "READY";
        String pgvectorStatus = "ENABLED (HNSW Index Active)";

        SemanticSearchStatusResponse semanticSearchStatus = new SemanticSearchStatusResponse(
                embeddingModel,
                embeddingDimension,
                totalEmbedded,
                searchStatus,
                pgvectorStatus
        );

        return new DashboardResponse(
                keyMetrics,
                charts,
                semanticSearchStatus,
                EXAMPLE_QUESTIONS
        );
    }

    public List<AgentStatusResponse> getAgents() {
        return List.of(
                new AgentStatusResponse(
                        "Supervisor Agent",
                        "Deterministic query intent routing and dispatching across specialized agents",
                        "AVAILABLE",
                        "Not Tracked"
                ),
                new AgentStatusResponse(
                        "Analytics Agent",
                        "Aggregates sentiment, category, theme, and priority distributions from structured feedback analysis",
                        "AVAILABLE",
                        "Not Tracked"
                ),
                new AgentStatusResponse(
                        "Retrieval Agent",
                        "Performs PGVector cosine semantic search over feedback embeddings using gemini-embedding-001",
                        "AVAILABLE",
                        "Not Tracked"
                ),
                new AgentStatusResponse(
                        "Data Quality Agent",
                        "Monitors ingestion batches, row validation failures, duplicate rates, and pipeline coverage",
                        "AVAILABLE",
                        "Not Tracked"
                ),
                new AgentStatusResponse(
                        "Reporting Agent",
                        "Synthesizes strictly evidence-grounded executive summaries and actionable recommendations via Gemini 2.5 Flash",
                        "AVAILABLE",
                        "Not Tracked"
                )
        );
    }

    private List<ChartDataPoint> toChartData(List<LabelCountProjection> projections, long total) {
        if (projections == null || projections.isEmpty()) {
            return List.of();
        }
        return projections.stream()
                .map(p -> {
                    String label = p.getLabel() != null ? String.valueOf(p.getLabel()) : "Unknown";
                    long count = p.getCount();
                    double percent = total > 0 ? Math.round((count * 1000.0) / total) / 10.0 : 0.0;
                    return new ChartDataPoint(label, count, percent);
                })
                .toList();
    }
}
