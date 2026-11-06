package com.customerfeedback.platform.dto;

import java.util.UUID;

public interface SemanticSearchProjection {
    UUID getFeedbackId();
    String getFeedbackText();
    Double getSimilarity();
    String getCategory();
    String getTheme();
    String getSentiment();
    String getPriority();
}
