package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.FeedbackAnalysisResponse;
import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.entity.IngestionBatch;
import com.customerfeedback.platform.model.enums.BatchStatus;
import com.customerfeedback.platform.model.enums.ProcessingStatus;
import com.customerfeedback.platform.repository.FeedbackAnalysisRepository;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngestionBatchRepository batchRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private FeedbackAnalysisRepository analysisRepository;

    @Autowired
    private com.customerfeedback.platform.repository.FeedbackEmbeddingRepository embeddingRepository;

    @Autowired
    private com.customerfeedback.platform.repository.ProcessingErrorRepository processingErrorRepository;

    @MockBean
    private AiAnalysisClient aiAnalysisClient;

    @MockBean
    private AiEmbeddingClient aiEmbeddingClient;

    private IngestionBatch testBatch;
    private Feedback testFeedback;

    @BeforeEach
    void setUp() {
        embeddingRepository.deleteAll();
        analysisRepository.deleteAll();
        feedbackRepository.deleteAll();
        processingErrorRepository.deleteAll();
        batchRepository.deleteAll();

        when(aiEmbeddingClient.requestEmbedding(anyString()))
                .thenReturn(new AiEmbeddingClient.EmbeddingResult("gemini-embedding-001", 768, "[0.123, -0.456]"));

        testBatch = new IngestionBatch("test_bank.csv", "WEB", "tester", "hash12345");
        testBatch.setStatus(BatchStatus.COMPLETED);
        testBatch = batchRepository.save(testBatch);

        testFeedback = new Feedback();
        testFeedback.setFeedbackId(UUID.randomUUID());
        testFeedback.setBatch(testBatch);
        testFeedback.setFeedbackDate(LocalDate.now());
        testFeedback.setChannel("Mobile App");
        testFeedback.setProduct("Checking Account");
        testFeedback.setRating(5);
        testFeedback.setCustomerSegment("Retail");
        testFeedback.setRegion("North");
        testFeedback.setFeedbackText("The mobile check deposit feature is instant and very easy to use.");
        testFeedback.setOriginalRowNumber(1);
        testFeedback.setProcessingStatus(ProcessingStatus.STORED);
        testFeedback = feedbackRepository.save(testFeedback);
    }

    @Test
    void shouldAnalyzeFeedbackAndPersistStructuredAnalysis() throws Exception {
        FeedbackAnalysisResponse mockAiResponse = new FeedbackAnalysisResponse(
                null,
                testFeedback.getFeedbackId(),
                "POSITIVE",
                "Mobile App",
                "Check Deposit",
                "Happy",
                "LOW",
                "Digital Banking",
                "Customer satisfied with quick mobile check deposit.",
                BigDecimal.valueOf(0.95),
                "gemini-2.5-flash",
                "gemini-2.5-flash",
                true,
                null
        );

        when(aiAnalysisClient.requestAnalysis(any(Feedback.class))).thenReturn(mockAiResponse);

        // 1. First analysis execution
        mockMvc.perform(post("/api/v1/analysis/feedback/" + testFeedback.getFeedbackId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackId", is(testFeedback.getFeedbackId().toString())))
                .andExpect(jsonPath("$.sentiment", is("POSITIVE")))
                .andExpect(jsonPath("$.category", is("Mobile App")))
                .andExpect(jsonPath("$.theme", is("Check Deposit")))
                .andExpect(jsonPath("$.emotion", is("Happy")))
                .andExpect(jsonPath("$.priority", is("LOW")))
                .andExpect(jsonPath("$.department", is("Digital Banking")))
                .andExpect(jsonPath("$.confidenceScore", is(0.95)))
                .andExpect(jsonPath("$.isCurrent", is(true)));

        verify(aiAnalysisClient, times(1)).requestAnalysis(any(Feedback.class));

        // 2. Second call without reanalyze - should skip Gemini and return existing analysis
        mockMvc.perform(post("/api/v1/analysis/feedback/" + testFeedback.getFeedbackId())
                        .param("reanalyze", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentiment", is("POSITIVE")));

        verify(aiAnalysisClient, times(1)).requestAnalysis(any(Feedback.class)); // Not called again!

        // 3. Re-analysis call with reanalyze=true - should call Gemini again and update isCurrent
        FeedbackAnalysisResponse updatedAiResponse = new FeedbackAnalysisResponse(
                null,
                testFeedback.getFeedbackId(),
                "POSITIVE",
                "Digital Banking",
                "Deposit Enhancement",
                "Happy",
                "LOW",
                "Mobile Engineering",
                "Customer pleased with newly updated deposit workflow.",
                BigDecimal.valueOf(0.98),
                "gemini-2.5-flash",
                "gemini-2.5-flash",
                true,
                null
        );
        when(aiAnalysisClient.requestAnalysis(any(Feedback.class))).thenReturn(updatedAiResponse);

        mockMvc.perform(post("/api/v1/analysis/feedback/" + testFeedback.getFeedbackId())
                        .param("reanalyze", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("Deposit Enhancement")))
                .andExpect(jsonPath("$.isCurrent", is(true)));

        verify(aiAnalysisClient, times(2)).requestAnalysis(any(Feedback.class));

        // 4. Verify version history preserves 2 records, only 1 is current
        var history = analysisRepository.findByFeedback_FeedbackIdOrderByCreatedAtDesc(testFeedback.getFeedbackId());
        assertEquals(2, history.size());
        assertTrue(history.get(0).isCurrent());
        assertFalse(history.get(1).isCurrent());

        // 5. Verify GET /api/v1/analysis/feedback/{id} returns the current one
        mockMvc.perform(get("/api/v1/analysis/feedback/" + testFeedback.getFeedbackId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("Deposit Enhancement")))
                .andExpect(jsonPath("$.isCurrent", is(true)));

        // 6. Verify GET /api/v1/analysis/batches/{batchId}
        mockMvc.perform(get("/api/v1/analysis/batches/" + testBatch.getBatchId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].theme", is("Deposit Enhancement")));
    }

    @Test
    void shouldAnalyzeBatchOfRecords() throws Exception {
        FeedbackAnalysisResponse mockAiResponse = new FeedbackAnalysisResponse(
                null,
                testFeedback.getFeedbackId(),
                "NEUTRAL",
                "General",
                "General Feedback",
                "Neutral",
                "MEDIUM",
                "Support",
                "General feedback statement.",
                BigDecimal.valueOf(0.80),
                "gemini-2.5-flash",
                "gemini-2.5-flash",
                true,
                null
        );
        when(aiAnalysisClient.requestAnalysis(any(Feedback.class))).thenReturn(mockAiResponse);

        mockMvc.perform(post("/api/v1/analysis/batches/" + testBatch.getBatchId())
                        .param("limit", "10")
                        .param("reanalyze", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId", is(testBatch.getBatchId().toString())))
                .andExpect(jsonPath("$.analyzedCount", is(1)))
                .andExpect(jsonPath("$.results", hasSize(1)));
    }
}
