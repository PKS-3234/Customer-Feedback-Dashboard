package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.RowValidationErrorDto;
import com.customerfeedback.platform.dto.ValidateCsvResponse;
import com.customerfeedback.platform.exception.FileValidationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class CsvValidationService {

    private static final Logger log = LoggerFactory.getLogger(CsvValidationService.class);

    public static final String COL_FEEDBACK_ID = "feedback_id";
    public static final String COL_FEEDBACK_DATE = "feedback_date";
    public static final String COL_CHANNEL = "channel";
    public static final String COL_PRODUCT = "product";
    public static final String COL_RATING = "rating";
    public static final String COL_CUSTOMER_SEGMENT = "customer_segment";
    public static final String COL_REGION = "region";
    public static final String COL_FEEDBACK_TEXT = "feedback_text";

    public static final Set<String> REQUIRED_COLUMNS = Set.of(
            COL_FEEDBACK_DATE,
            COL_CHANNEL,
            COL_PRODUCT,
            COL_FEEDBACK_TEXT
    );

    @Value("${app.ingestion.min-text-length:3}")
    private int minTextLength = 3;

    @Value("${app.ingestion.max-text-length:5000}")
    private int maxTextLength = 5000;

    public void validateFileStructure(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("The uploaded file is empty or missing.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new FileValidationException("File must have a .csv extension.");
        }
    }

    public ValidateCsvResponse previewAndValidate(MultipartFile file) {
        validateFileStructure(file);

        try (InputStream is = file.getInputStream();
             Reader reader = createBomAwareReader(is)) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(reader);
            List<String> rawHeaders = parser.getHeaderNames();

            if (rawHeaders == null || rawHeaders.isEmpty()) {
                throw new FileValidationException("The CSV file does not contain a valid header row.");
            }

            Map<String, String> headerMap = new HashMap<>();
            for (String header : rawHeaders) {
                String cleanHeader = stripBom(header).trim();
                headerMap.put(cleanHeader.toLowerCase(), cleanHeader);
            }

            // Check required columns
            List<String> missingHeaders = new ArrayList<>();
            for (String required : REQUIRED_COLUMNS) {
                if (!headerMap.containsKey(required.toLowerCase())) {
                    missingHeaders.add(required);
                }
            }

            boolean headerValidation = missingHeaders.isEmpty();
            if (!headerValidation) {
                throw new FileValidationException("Missing required CSV header columns: " + String.join(", ", missingHeaders));
            }

            int totalRows = 0;
            int validRows = 0;
            int invalidRows = 0;
            List<RowValidationErrorDto> sampleErrors = new ArrayList<>();

            for (CSVRecord record : parser) {
                if (isRecordCompletelyBlank(record)) {
                    continue;
                }

                totalRows++;
                int rowNumber = (int) record.getRecordNumber();
                List<RowValidationErrorDto> rowErrors = validateRecord(record, headerMap, rowNumber);

                if (rowErrors.isEmpty()) {
                    validRows++;
                } else {
                    invalidRows++;
                    if (sampleErrors.size() < 10) {
                        sampleErrors.addAll(rowErrors);
                    }
                }
            }

            boolean isValid = (invalidRows == 0 && totalRows > 0);
            return new ValidateCsvResponse(isValid, totalRows, validRows, invalidRows, headerValidation, sampleErrors);

        } catch (FileValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse and validate CSV file", e);
            throw new FileValidationException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    public List<RowValidationErrorDto> validateRecord(CSVRecord record, Map<String, String> headerMap, int rowNumber) {
        List<RowValidationErrorDto> errors = new ArrayList<>();
        String rawRecord = record.toString();

        // 1. feedback_id (optional, but if provided must be valid UUID)
        String feedbackIdStr = getFieldValue(record, headerMap, COL_FEEDBACK_ID);
        if (feedbackIdStr != null && !feedbackIdStr.isBlank()) {
            try {
                UUID.fromString(feedbackIdStr.trim());
            } catch (IllegalArgumentException e) {
                errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_ID,
                        "Invalid UUID format for feedback_id: " + feedbackIdStr, rawRecord));
            }
        }

        // 2. feedback_date (required, valid date format)
        String feedbackDateStr = getFieldValue(record, headerMap, COL_FEEDBACK_DATE);
        if (feedbackDateStr == null || feedbackDateStr.isBlank()) {
            errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_DATE,
                    "Required field 'feedback_date' is missing or empty.", rawRecord));
        } else {
            LocalDate parsedDate = parseDate(feedbackDateStr);
            if (parsedDate == null) {
                errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_DATE,
                        "Invalid date format: '" + feedbackDateStr + "'. Expected YYYY-MM-DD.", rawRecord));
            }
        }

        // 3. channel (required)
        String channel = getFieldValue(record, headerMap, COL_CHANNEL);
        if (channel == null || channel.isBlank()) {
            errors.add(new RowValidationErrorDto(rowNumber, COL_CHANNEL,
                    "Required field 'channel' is missing or empty.", rawRecord));
        }

        // 4. product (required)
        String product = getFieldValue(record, headerMap, COL_PRODUCT);
        if (product == null || product.isBlank()) {
            errors.add(new RowValidationErrorDto(rowNumber, COL_PRODUCT,
                    "Required field 'product' is missing or empty.", rawRecord));
        }

        // 5. rating (validate between 1 and 5)
        String ratingStr = getFieldValue(record, headerMap, COL_RATING);
        if (ratingStr != null && !ratingStr.isBlank()) {
            try {
                int rating = Integer.parseInt(ratingStr.trim());
                if (rating < 1 || rating > 5) {
                    errors.add(new RowValidationErrorDto(rowNumber, COL_RATING,
                            "Rating value " + rating + " must be between 1 and 5.", rawRecord));
                }
            } catch (NumberFormatException e) {
                errors.add(new RowValidationErrorDto(rowNumber, COL_RATING,
                        "Rating value '" + ratingStr + "' is not a valid integer.", rawRecord));
            }
        }

        // 6. feedback_text (required, reasonable length)
        String feedbackText = getFieldValue(record, headerMap, COL_FEEDBACK_TEXT);
        if (feedbackText == null || feedbackText.isBlank()) {
            errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_TEXT,
                    "Required field 'feedback_text' is missing or empty.", rawRecord));
        } else {
            String trimmed = feedbackText.trim();
            if (trimmed.length() < minTextLength) {
                errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_TEXT,
                        "Feedback text length (" + trimmed.length() + ") is less than minimum allowed (" + minTextLength + ").", rawRecord));
            } else if (trimmed.length() > maxTextLength) {
                errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_TEXT,
                        "Feedback text length (" + trimmed.length() + ") exceeds maximum allowed (" + maxTextLength + ").", rawRecord));
            }
        }

        return errors;
    }

    public String getFieldValue(CSVRecord record, Map<String, String> headerMap, String canonicalName) {
        String actualHeader = headerMap.get(canonicalName.toLowerCase());
        if (actualHeader != null && record.isMapped(actualHeader)) {
            String val = record.get(actualHeader);
            return val != null ? val.trim() : null;
        }
        return null;
    }

    public LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String clean = dateStr.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(clean, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    public Integer parseRating(String ratingStr) {
        if (ratingStr == null || ratingStr.isBlank()) {
            return null;
        }
        try {
            int val = Integer.parseInt(ratingStr.trim());
            return (val >= 1 && val <= 5) ? val : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isRecordCompletelyBlank(CSVRecord record) {
        for (String val : record) {
            if (val != null && !val.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public Reader createBomAwareReader(InputStream is) throws IOException {
        PushbackInputStream pbis = new PushbackInputStream(new BufferedInputStream(is), 4);
        byte[] bom = new byte[3];
        int n = pbis.read(bom, 0, bom.length);
        if (n == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            // Bom consumed
        } else if (n > 0) {
            pbis.unread(bom, 0, n);
        }
        return new InputStreamReader(pbis, StandardCharsets.UTF_8);
    }

    public String stripBom(String s) {
        if (s == null) return "";
        if (s.startsWith("\uFEFF")) {
            return s.substring(1);
        }
        return s;
    }
}
