package com.customerfeedback.platform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldValidateCsvWithoutPersisting() throws Exception {
        String csvContent = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                fb000000-0000-0000-0000-000000000010,2026-11-01,Mobile App,Checking,5,Retail,North,Superb customer service!
                """;
        MockMultipartFile file = new MockMultipartFile("file", "test_preview.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/batches/validate").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.rowCount", is(1)))
                .andExpect(jsonPath("$.validRows", is(1)))
                .andExpect(jsonPath("$.invalidRows", is(0)))
                .andExpect(jsonPath("$.headerValidation", is(true)));
    }

    @Test
    void shouldUploadAndProcessBatchWithDuplicateDetectionAndErrors() throws Exception {
        String csvContent = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                fb000000-0000-0000-0000-000000000021,2026-11-01,Mobile,Checking,5,Retail,North,Great banking app and intuitive UI!
                fb000000-0000-0000-0000-000000000022,2026-11-02,Branch,Mortgage,1,Wealth,West,Long lines and rude staff member.
                fb000000-0000-0000-0000-000000000023,2026-11-03,Branch,Mortgage,5,Retail,East,Great banking app and intuitive UI!
                fb000000-0000-0000-0000-000000000024,invalid-date,Branch,Loan,3,Retail,South,Good experience overall.
                """;
        MockMultipartFile file = new MockMultipartFile("file", "batch_test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/v1/batches")
                        .file(file)
                        .param("sourceSystem", "TEST_SUITE")
                        .param("uploadUser", "qa_engineer"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchId", notNullValue()))
                .andExpect(jsonPath("$.totalRows", is(4)))
                .andExpect(jsonPath("$.successfulRows", is(2)))
                .andExpect(jsonPath("$.duplicateRows", is(1)))
                .andExpect(jsonPath("$.failedRows", is(1)))
                .andExpect(jsonPath("$.status", is("COMPLETED_WITH_ERRORS")))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String batchId = com.jayway.jsonpath.JsonPath.read(responseBody, "$.batchId");

        // 1. Verify GET /api/v1/batches/{batchId}
        mockMvc.perform(get("/api/v1/batches/" + batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId", is(batchId)))
                .andExpect(jsonPath("$.sourceFileName", is("batch_test.csv")))
                .andExpect(jsonPath("$.successfulRows", is(2)))
                .andExpect(jsonPath("$.duplicateRows", is(1)))
                .andExpect(jsonPath("$.failedRows", is(1)));

        // 2. Verify GET /api/v1/batches/{batchId}/errors
        mockMvc.perform(get("/api/v1/batches/" + batchId + "/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

        // 3. Verify GET /api/v1/batches list
        mockMvc.perform(get("/api/v1/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

        // 4. Verify GET /api/v1/feedback by batchId
        mockMvc.perform(get("/api/v1/feedback?batchId=" + batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
    }
}
