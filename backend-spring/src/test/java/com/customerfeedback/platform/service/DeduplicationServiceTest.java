package com.customerfeedback.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicationServiceTest {

    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        deduplicationService = new DeduplicationService();
    }

    @Test
    void shouldComputeConsistentFileHash() {
        byte[] content = "feedback_id,feedback_date,channel,product,rating,feedback_text\n".getBytes(StandardCharsets.UTF_8);
        String hash1 = deduplicationService.computeFileHash(content);
        String hash2 = deduplicationService.computeFileHash(content);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }

    @Test
    void shouldNormalizeFeedbackText() {
        String input = "  Great   Mobile Banking   App!! \n  ";
        String normalized = deduplicationService.normalizeFeedbackText(input);

        assertEquals("great mobile banking app!!", normalized);
    }

    @Test
    void shouldHandleNullFeedbackText() {
        assertEquals("", deduplicationService.normalizeFeedbackText(null));
    }
}
