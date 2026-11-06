package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.SemanticSearchResultResponse;
import com.customerfeedback.platform.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SemanticSearchController.class)
class SemanticSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmbeddingService embeddingService;

    @Test
    void search_validQuery_returnsRankedResults() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        List<SemanticSearchResultResponse> mockResults = List.of(
                new SemanticSearchResultResponse(
                        feedbackId,
                        "Duplicate card charge appeared twice on my statement.",
                        0.9452,
                        "Cards",
                        "Duplicate Charge",
                        "NEGATIVE",
                        "HIGH"
                )
        );

        when(embeddingService.performSemanticSearch(eq("duplicate card charge"), anyInt()))
                .thenReturn(mockResults);

        String payload = """
            {
              "query": "duplicate card charge",
              "limit": 5
            }
        """;

        mockMvc.perform(post("/api/v1/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].feedbackId").value(feedbackId.toString()))
                .andExpect(jsonPath("$[0].feedbackText").value("Duplicate card charge appeared twice on my statement."))
                .andExpect(jsonPath("$[0].similarity").value(0.9452))
                .andExpect(jsonPath("$[0].category").value("Cards"))
                .andExpect(jsonPath("$[0].theme").value("Duplicate Charge"))
                .andExpect(jsonPath("$[0].sentiment").value("NEGATIVE"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));
    }

    @Test
    void search_blankQuery_returnsBadRequest() throws Exception {
        String payload = """
            {
              "query": "   ",
              "limit": 5
            }
        """;

        mockMvc.perform(post("/api/v1/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_invalidLimit_returnsBadRequest() throws Exception {
        String payload = """
            {
              "query": "valid query",
              "limit": 0
            }
        """;

        mockMvc.perform(post("/api/v1/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
