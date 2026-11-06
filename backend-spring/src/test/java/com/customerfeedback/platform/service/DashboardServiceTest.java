package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.DashboardResponse;
import com.customerfeedback.platform.dto.LabelCountProjection;
import com.customerfeedback.platform.entity.FeedbackEmbedding;
import com.customerfeedback.platform.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private FeedbackAnalysisRepository feedbackAnalysisRepository;

    @Mock
    private FeedbackEmbeddingRepository feedbackEmbeddingRepository;

    @Mock
    private IngestionBatchRepository ingestionBatchRepository;

    @Mock
    private ProcessingErrorRepository processingErrorRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardMetrics_withData_calculatesCorrectPercentages() {
        when(feedbackRepository.count()).thenReturn(10L);
        when(feedbackAnalysisRepository.countByIsCurrentTrue()).thenReturn(8L);
        when(feedbackEmbeddingRepository.countByIsCurrentTrue()).thenReturn(8L);

        when(feedbackAnalysisRepository.countByIsCurrentTrueAndSentimentIgnoreCase("POSITIVE")).thenReturn(5L);
        when(feedbackAnalysisRepository.countByIsCurrentTrueAndSentimentIgnoreCase("NEGATIVE")).thenReturn(3L);
        when(feedbackAnalysisRepository.countByIsCurrentTrueAndPriorityIgnoreCase("HIGH")).thenReturn(2L);

        when(ingestionBatchRepository.count()).thenReturn(2L);
        when(processingErrorRepository.count()).thenReturn(1L);
        when(ingestionBatchRepository.sumFailedRows()).thenReturn(1L);
        when(ingestionBatchRepository.sumDuplicateRows()).thenReturn(0L);

        LabelCountProjection sentimentPositive = mockProjection("POSITIVE", 5L);
        LabelCountProjection sentimentNegative = mockProjection("NEGATIVE", 3L);
        when(feedbackAnalysisRepository.findSentimentDistribution()).thenReturn(List.of(sentimentPositive, sentimentNegative));

        LabelCountProjection categoryMock = mockProjection("Mobile App", 8L);
        when(feedbackAnalysisRepository.findCategoryDistribution()).thenReturn(List.of(categoryMock));

        LabelCountProjection themeMock = mockProjection("Login Speed", 8L);
        when(feedbackAnalysisRepository.findThemeDistribution()).thenReturn(List.of(themeMock));

        LabelCountProjection priorityMock = mockProjection("HIGH", 2L);
        when(feedbackAnalysisRepository.findPriorityDistribution()).thenReturn(List.of(priorityMock));

        LabelCountProjection channelMock = mockProjection("MOBILE_APP", 10L);
        when(feedbackRepository.findChannelDistribution()).thenReturn(List.of(channelMock));

        LabelCountProjection productMock = mockProjection("Checking Account", 10L);
        when(feedbackRepository.findProductDistribution()).thenReturn(List.of(productMock));

        LabelCountProjection errorMock = mockProjection("RATING_INVALID", 1L);
        when(processingErrorRepository.findErrorDistribution()).thenReturn(List.of(errorMock));

        FeedbackEmbedding embedding = new FeedbackEmbedding();
        embedding.setEmbeddingModel("gemini-embedding-001");
        embedding.setEmbeddingDimension(768);
        when(feedbackEmbeddingRepository.findFirstByIsCurrentTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(embedding));

        DashboardResponse response = dashboardService.getDashboardMetrics();

        assertThat(response).isNotNull();
        assertThat(response.keyMetrics().totalFeedbackRecords()).isEqualTo(10L);
        assertThat(response.keyMetrics().totalAnalyzedRecords()).isEqualTo(8L);
        assertThat(response.keyMetrics().totalEmbeddedRecords()).isEqualTo(8L);
        assertThat(response.keyMetrics().positiveFeedbackPercent()).isEqualTo(62.5);
        assertThat(response.keyMetrics().negativeFeedbackPercent()).isEqualTo(37.5);
        assertThat(response.keyMetrics().highPriorityFeedbackCount()).isEqualTo(2L);
        assertThat(response.keyMetrics().currentBatches()).isEqualTo(2L);
        assertThat(response.keyMetrics().processingErrors()).isEqualTo(1L);
        assertThat(response.keyMetrics().failedRows()).isEqualTo(1L);
        assertThat(response.keyMetrics().duplicateRows()).isEqualTo(0L);

        assertThat(response.charts().sentimentDistribution()).hasSize(2);
        assertThat(response.charts().sentimentDistribution().get(0).label()).isEqualTo("POSITIVE");
        assertThat(response.charts().sentimentDistribution().get(0).percentage()).isEqualTo(62.5);
        assertThat(response.charts().errorDistribution()).hasSize(1);

        assertThat(response.semanticSearchStatus().embeddingModel()).isEqualTo("gemini-embedding-001");
        assertThat(response.semanticSearchStatus().embeddingDimension()).isEqualTo(768);
        assertThat(response.semanticSearchStatus().semanticSearchStatus()).isEqualTo("OPERATIONAL");
        assertThat(response.semanticSearchStatus().pgvectorStatus()).isEqualTo("ENABLED (HNSW Index Active)");
    }

    @Test
    void getDashboardMetrics_zeroData_handlesSafely() {
        when(feedbackRepository.count()).thenReturn(0L);
        when(feedbackAnalysisRepository.countByIsCurrentTrue()).thenReturn(0L);
        when(feedbackEmbeddingRepository.countByIsCurrentTrue()).thenReturn(0L);

        when(feedbackAnalysisRepository.countByIsCurrentTrueAndSentimentIgnoreCase("POSITIVE")).thenReturn(0L);
        when(feedbackAnalysisRepository.countByIsCurrentTrueAndSentimentIgnoreCase("NEGATIVE")).thenReturn(0L);
        when(feedbackAnalysisRepository.countByIsCurrentTrueAndPriorityIgnoreCase("HIGH")).thenReturn(0L);

        when(ingestionBatchRepository.count()).thenReturn(0L);
        when(processingErrorRepository.count()).thenReturn(0L);
        when(ingestionBatchRepository.sumFailedRows()).thenReturn(0L);
        when(ingestionBatchRepository.sumDuplicateRows()).thenReturn(0L);

        when(feedbackEmbeddingRepository.findFirstByIsCurrentTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        DashboardResponse response = dashboardService.getDashboardMetrics();

        assertThat(response.keyMetrics().totalFeedbackRecords()).isZero();
        assertThat(response.keyMetrics().positiveFeedbackPercent()).isZero();
        assertThat(response.keyMetrics().negativeFeedbackPercent()).isZero();
        assertThat(response.semanticSearchStatus().semanticSearchStatus()).isEqualTo("READY");
    }

    @Test
    void getAgents_returnsFiveConfiguredAgents() {
        var agents = dashboardService.getAgents();
        assertThat(agents).hasSize(5);
        assertThat(agents).extracting("name")
                .containsExactly(
                        "Supervisor Agent",
                        "Analytics Agent",
                        "Retrieval Agent",
                        "Data Quality Agent",
                        "Reporting Agent"
                );
        assertThat(agents).allMatch(a -> "AVAILABLE".equals(a.availability()));
        assertThat(agents).allMatch(a -> "Not Tracked".equals(a.executionHistory()));
    }

    private LabelCountProjection mockProjection(String label, long count) {
        return new LabelCountProjection() {
            @Override
            public Object getLabel() {
                return label;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
