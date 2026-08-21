package com.example.wasaas.whatsapp.webhook;

import com.example.wasaas.whatsapp.meta.MetaProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {

    private final MetaProperties metaProperties;

    public WebhookSignatureVerifier(MetaProperties metaProperties) {
        this.metaProperties = metaProperties;
    }

    public boolean isValid(byte[] rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=") || rawBody == null) {
            return false;
        }

        String appSecret = metaProperties.getAppSecret();
        if (appSecret == null || appSecret.isBlank()) {
            return false;
        }

        String providedSignature = signatureHeader.substring("sha256=".length()).trim();

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computedBytes = mac.doFinal(rawBody);
            String computedHex = HexFormat.of().formatHex(computedBytes);

            // Constant-time comparison to defend against timing attacks
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
