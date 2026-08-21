package com.example.wasaas.ledger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PhonePrivacyUtils {

    private PhonePrivacyUtils() {}

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    public static String hashPhoneNumber(String phoneNumber) {
        String normalized = normalize(phoneNumber);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String extractLast4(String phoneNumber) {
        String normalized = normalize(phoneNumber);
        if (normalized.length() < 4) {
            return String.format("%4s", normalized).replace(' ', '0');
        }
        return normalized.substring(normalized.length() - 4);
    }
}
