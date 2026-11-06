package com.customerfeedback.platform.dto;

public record RowValidationErrorDto(
        int rowNumber,
        String field,
        String message,
        String rawRecord
) {}
