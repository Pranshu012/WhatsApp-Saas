package com.example.wasaas.whatsapp.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TokenCipherTest {

    private static final String VALID_32_BYTE_KEY_BASE64 = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private TokenCipher tokenCipher;

    @BeforeEach
    void setup() {
        tokenCipher = new TokenCipher(VALID_32_BYTE_KEY_BASE64);
        tokenCipher.validateAndInitializeKey();
    }

    @Test
    void testEncryptDecryptRoundTrip() {
        String originalToken = "EAAB1234567890abcdefghijklmnopqrstuvwxyz_SECRET_META_ACCESS_TOKEN";

        byte[] encrypted = tokenCipher.encrypt(originalToken);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted.length).isGreaterThan(12 + 16); // IV + Tag + Data

        String decrypted = tokenCipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(originalToken);
    }

    @Test
    void testCiphertextDiffersAcrossEncryptions() {
        String token = "EAAB_REPEATABLE_PLAINTEXT_TOKEN";

        byte[] enc1 = tokenCipher.encrypt(token);
        byte[] enc2 = tokenCipher.encrypt(token);

        // Even though plaintext is identical, random 12-byte IV ensures ciphertext differs
        assertThat(enc1).isNotEqualTo(enc2);

        // Both decrypt back to the same plaintext
        assertThat(tokenCipher.decrypt(enc1)).isEqualTo(token);
        assertThat(tokenCipher.decrypt(enc2)).isEqualTo(token);
    }

    @Test
    void testTamperedCiphertextFailsDecryption() {
        String token = "EAAB_SENSITIVE_TOKEN";
        byte[] encrypted = tokenCipher.encrypt(token);

        // Tamper with ciphertext byte
        encrypted[encrypted.length - 1] ^= 0xFF;

        assertThatThrownBy(() -> tokenCipher.decrypt(encrypted))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decrypt access token");
    }

    @Test
    void testDecryptionWithWrongKeyFails() {
        String token = "EAAB_SENSITIVE_TOKEN";
        byte[] encrypted = tokenCipher.encrypt(token);

        // Another valid 32-byte key
        String wrongKey = Base64.getEncoder().encodeToString(new byte[32]);
        TokenCipher otherCipher = new TokenCipher(wrongKey);
        otherCipher.validateAndInitializeKey();

        assertThatThrownBy(() -> otherCipher.decrypt(encrypted))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decrypt access token");
    }

    @Test
    void testMissingKeyFailsStartup() {
        TokenCipher nullKeyCipher = new TokenCipher(null);
        assertThatThrownBy(nullKeyCipher::validateAndInitializeKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOKEN_ENCRYPTION_KEY environment variable is missing or empty");

        TokenCipher emptyKeyCipher = new TokenCipher("   ");
        assertThatThrownBy(emptyKeyCipher::validateAndInitializeKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOKEN_ENCRYPTION_KEY environment variable is missing or empty");
    }

    @Test
    void testInvalidKeyLengthFailsStartup() {
        // 16 bytes key (128-bit, invalid because we require 256-bit / 32 bytes)
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        TokenCipher shortCipher = new TokenCipher(shortKey);

        assertThatThrownBy(shortCipher::validateAndInitializeKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOKEN_ENCRYPTION_KEY must be exactly 32 bytes");
    }

    @Test
    void testMalformedBase64KeyFailsStartup() {
        TokenCipher malformedCipher = new TokenCipher("not-a-valid-base64-string!@#$");

        assertThatThrownBy(malformedCipher::validateAndInitializeKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be a valid Base64 encoded string");
    }
}
