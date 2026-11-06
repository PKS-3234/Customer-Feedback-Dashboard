package com.customerfeedback.platform.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class DeduplicationService {

    /**
     * Computes SHA-256 hash over file bytes.
     */
    public String computeFileHash(byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content cannot be null or empty when computing hash");
        }
        return sha256(content);
    }

    /**
     * Normalizes feedback text by trimming, lowercasing, and collapsing internal whitespace.
     */
    public String normalizeFeedbackText(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in environment", e);
        }
    }
}
