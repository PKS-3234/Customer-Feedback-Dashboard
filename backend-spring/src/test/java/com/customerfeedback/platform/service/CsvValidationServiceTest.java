package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.ValidateCsvResponse;
import com.customerfeedback.platform.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CsvValidationServiceTest {

    private CsvValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new CsvValidationService();
    }

    @Test
    void shouldValidateValidCsvSuccessfully() {
        String csvContent = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                fb000000-0000-0000-0000-000000000001,2026-11-01,Mobile App,Checking Account,5,Retail,North,Fast transfer and clean interface!
                fb000000-0000-0000-0000-000000000002,2026-11-02,Branch,Mortgage,2,Wealth,West,Long waiting queue at the branch desk.
                """;
        MockMultipartFile file = new MockMultipartFile("file", "valid.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        ValidateCsvResponse response = validationService.previewAndValidate(file);

        assertTrue(response.valid());
        assertEquals(2, response.rowCount());
        assertEquals(2, response.validRows());
        assertEquals(0, response.invalidRows());
        assertTrue(response.headerValidation());
        assertTrue(response.sampleErrors().isEmpty());
    }

    @Test
    void shouldCatchMissingRequiredHeaders() {
        String csvContent = """
                rating,customer_segment,region,notes
                5,Retail,North,Missing channel and date
                """;
        MockMultipartFile file = new MockMultipartFile("file", "bad_header.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        assertThrows(FileValidationException.class, () -> validationService.previewAndValidate(file));
    }

    @Test
    void shouldDetectInvalidRatingAndMissingText() {
        String csvContent = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                fb000000-0000-0000-0000-000000000003,2026-11-03,Email,Credit Card,9,Retail,East,Invalid rating 9
                fb000000-0000-0000-0000-000000000004,2026-11-04,Web,Savings,4,SME,South,
                """;
        MockMultipartFile file = new MockMultipartFile("file", "invalid_rows.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        ValidateCsvResponse response = validationService.previewAndValidate(file);

        assertFalse(response.valid());
        assertEquals(2, response.rowCount());
        assertEquals(0, response.validRows());
        assertEquals(2, response.invalidRows());
        assertEquals(2, response.sampleErrors().size());
    }

    @Test
    void shouldDetectInvalidDateFormat() {
        String csvContent = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                fb000000-0000-0000-0000-000000000005,not-a-date,Mobile App,Loan,3,Retail,North,Feedback with bad date
                """;
        MockMultipartFile file = new MockMultipartFile("file", "bad_date.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        ValidateCsvResponse response = validationService.previewAndValidate(file);

        assertFalse(response.valid());
        assertEquals(1, response.invalidRows());
        assertTrue(response.sampleErrors().getFirst().message().contains("Invalid date format"));
    }
}
