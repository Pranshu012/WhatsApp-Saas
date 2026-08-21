package com.example.wasaas.whatsapp.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM Envelope Encryption for WhatsApp Access Tokens.
 * A fresh 12-byte random IV/nonce is generated for every encryption and stored
 * alongside the ciphertext.
 */
@Component
public class TokenCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BYTES = 32;

    private final String keyBase64;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    public TokenCipher(@Value("${app.crypto.token-key:}") String keyBase64) {
        this.keyBase64 = keyBase64;
    }

    @PostConstruct
    public void validateAndInitializeKey() {
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalStateException(
                    "TOKEN_ENCRYPTION_KEY environment variable is missing or empty. Application cannot start insecurely.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("TOKEN_ENCRYPTION_KEY must be a valid Base64 encoded string", e);
        }

        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "TOKEN_ENCRYPTION_KEY must be exactly 32 bytes (256 bits). Found: " + keyBytes.length + " bytes.");
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Plaintext to encrypt cannot be null or empty");
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH_BYTES + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);
            return buffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt access token", e);
        }
    }

    public String decrypt(byte[] combined) {
        if (combined == null || combined.length < (IV_LENGTH_BYTES + (TAG_LENGTH_BITS / 8))) {
            throw new IllegalArgumentException("Ciphertext is too short or malformed");
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt access token", e);
        }
    }
}
