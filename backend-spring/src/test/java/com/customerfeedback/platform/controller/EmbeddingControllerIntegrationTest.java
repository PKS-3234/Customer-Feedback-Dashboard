package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.entity.FeedbackAnalysis;
import com.customerfeedback.platform.entity.IngestionBatch;
import com.customerfeedback.platform.model.enums.BatchStatus;
import com.customerfeedback.platform.model.enums.ProcessingStatus;
import com.customerfeedback.platform.repository.FeedbackAnalysisRepository;
import com.customerfeedback.platform.repository.FeedbackEmbeddingRepository;
import com.customerfeedback.platform.repository.FeedbackRepository;
import com.customerfeedback.platform.repository.IngestionBatchRepository;
import com.customerfeedback.platform.service.AiAnalysisClient;
import com.customerfeedback.platform.service.AiEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmbeddingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngestionBatchRepository batchRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private FeedbackAnalysisRepository analysisRepository;

    @Autowired
    private FeedbackEmbeddingRepository embeddingRepository;

    @Autowired
    private com.customerfeedback.platform.repository.ProcessingErrorRepository processingErrorRepository;

    @MockBean
    private AiAnalysisClient aiAnalysisClient;

    @MockBean
    private AiEmbeddingClient aiEmbeddingClient;

    private IngestionBatch testBatch;
    private Feedback testFeedback;
    private FeedbackAnalysis testAnalysis;

    @BeforeEach
    void setUp() {
        embeddingRepository.deleteAll();
        analysisRepository.deleteAll();
        feedbackRepository.deleteAll();
        processingErrorRepository.deleteAll();
        batchRepository.deleteAll();

        testBatch = new IngestionBatch("test_batch.csv", "PORTAL", "operator", "hash123");
        testBatch.setStatus(BatchStatus.COMPLETED);
        testBatch = batchRepository.save(testBatch);

        testFeedback = new Feedback();
        testFeedback.setFeedbackId(UUID.randomUUID());
        testFeedback.setBatch(testBatch);
        testFeedback.setFeedbackDate(LocalDate.now());
        testFeedback.setChannel("Mobile App");
        testFeedback.setProduct("Checking Account");
        testFeedback.setRating(5);
        testFeedback.setFeedbackText("The mobile app instant transfer is remarkably swift.");
        testFeedback.setOriginalRowNumber(1);
        testFeedback.setProcessingStatus(ProcessingStatus.STORED);
        testFeedback = feedbackRepository.save(testFeedback);

        testAnalysis = new FeedbackAnalysis();
        testAnalysis.setAnalysisId(UUID.randomUUID());
        testAnalysis.setFeedback(testFeedback);
        testAnalysis.setSentiment("POSITIVE");
        testAnalysis.setCategory("Mobile App");
        testAnalysis.setTheme("Instant Transfer");
        testAnalysis.setEmotion("Happy");
        testAnalysis.setPriority("LOW");
        testAnalysis.setDepartment("Digital Banking");
        testAnalysis.setShortSummary("Customer is satisfied with swift mobile transfer.");
        testAnalysis.setConfidenceScore(BigDecimal.valueOf(0.98));
        testAnalysis.setModelName("gemini-2.5-flash");
        testAnalysis.setModelVersion("gemini-2.5-flash");
        testAnalysis.setCurrent(true);
        testAnalysis = analysisRepository.save(testAnalysis);
    }

    @Test
    void generateFeedbackEmbedding_success_returnsMetadataWithoutRawVector() throws Exception {
        when(aiEmbeddingClient.requestEmbedding(anyString()))
                .thenReturn(new AiEmbeddingClient.EmbeddingResult("gemini-embedding-001", 768, "[0.123, -0.456]"));

        mockMvc.perform(post("/api/v1/embeddings/feedback/" + testFeedback.getFeedbackId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embeddingId").isNotEmpty())
                .andExpect(jsonPath("$.feedbackId").value(testFeedback.getFeedbackId().toString()))
                .andExpect(jsonPath("$.analysisId").value(testAnalysis.getAnalysisId().toString()))
                .andExpect(jsonPath("$.embeddingModel").value("gemini-embedding-001"))
                .andExpect(jsonPath("$.embeddingDimension").value(768))
                .andExpect(jsonPath("$.isCurrent").value(true))
                .andExpect(jsonPath("$.embedding").doesNotExist()) // Raw vector must not be returned
                .andExpect(jsonPath("$.vector").doesNotExist());

        verify(aiEmbeddingClient, times(1)).requestEmbedding(anyString());
    }

    @Test
    void generateFeedbackEmbedding_skipIfAlreadyExists() throws Exception {
        when(aiEmbeddingClient.requestEmbedding(anyString()))
                .thenReturn(new AiEmbeddingClient.EmbeddingResult("gemini-embedding-001", 768, "[0.123, -0.456]"));

        // First call generates
        mockMvc.perform(post("/api/v1/embeddings/feedback/" + testFeedback.getFeedbackId()))
                .andExpect(status().isOk());

        // Second call skips Gemini
        mockMvc.perform(post("/api/v1/embeddings/feedback/" + testFeedback.getFeedbackId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embeddingDimension").value(768));

        verify(aiEmbeddingClient, times(1)).requestEmbedding(anyString());
    }

    @Test
    void generateFeedbackEmbedding_forceRegenerates() throws Exception {
        when(aiEmbeddingClient.requestEmbedding(anyString()))
                .thenReturn(new AiEmbeddingClient.EmbeddingResult("gemini-embedding-001", 768, "[0.123, -0.456]"));

        // First call
        mockMvc.perform(post("/api/v1/embeddings/feedback/" + testFeedback.getFeedbackId()))
                .andExpect(status().isOk());

        // Second call with force=true
        mockMvc.perform(post("/api/v1/embeddings/feedback/" + testFeedback.getFeedbackId() + "?force=true"))
                .andExpect(status().isOk());

        verify(aiEmbeddingClient, times(2)).requestEmbedding(anyString());
    }

    @Test
    void getFeedbackEmbedding_returnsMetadata() throws Exception {
        when(aiEmbeddingClient.requestEmbedding(anyString()))
                .thenReturn(new AiEmbeddingClient.EmbeddingResult("gemini-embedding-001", 768, "[0.123, -0.456]"));

        mockMvc.perform(post("/api/v1/embeddings/feedback/" + testFeedback.getFeedbackId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/embeddings/feedback/" + testFeedback.getFeedbackId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackId").value(testFeedback.getFeedbackId().toString()))
                .andExpect(jsonPath("$.isCurrent").value(true));
    }

    @Test
    void generateBatchEmbeddings_returnsSummary() throws Exception {
        when(aiEmbeddingClient.requestEmbedding(anyString()))
                .thenReturn(new AiEmbeddingClient.EmbeddingResult("gemini-embedding-001", 768, "[0.123, -0.456]"));

        mockMvc.perform(post("/api/v1/embeddings/batches/" + testBatch.getBatchId() + "?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(testBatch.getBatchId().toString()))
                .andExpect(jsonPath("$.totalRequested").value(1))
                .andExpect(jsonPath("$.generatedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.results", hasSize(1)));
    }
}
