package com.customerfeedback.platform.dto;

import java.util.List;

public record ValidateCsvResponse(
        boolean valid,
        int rowCount,
        int validRows,
        int invalidRows,
        boolean headerValidation,
        List<RowValidationErrorDto> sampleErrors
) {}
