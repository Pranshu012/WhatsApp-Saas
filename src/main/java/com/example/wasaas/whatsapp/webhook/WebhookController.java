package com.example.wasaas.whatsapp.webhook;

import com.example.wasaas.whatsapp.meta.MetaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookIngestService ingestService;
    private final MetaProperties metaProperties;

    public WebhookController(WebhookSignatureVerifier signatureVerifier,
                             WebhookIngestService ingestService,
                             MetaProperties metaProperties) {
        this.signatureVerifier = signatureVerifier;
        this.ingestService = ingestService;
        this.metaProperties = metaProperties;
    }

    @GetMapping
    public ResponseEntity<String> verifyHandshake(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        String expectedToken = metaProperties.getWebhookVerifyToken();
        if (expectedToken == null || expectedToken.isBlank() || mode == null || verifyToken == null || challenge == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean isValidMode = "subscribe".equals(mode);
        boolean isTokenValid = MessageDigest.isEqual(
                verifyToken.getBytes(StandardCharsets.UTF_8),
                expectedToken.getBytes(StandardCharsets.UTF_8)
        );

        if (isValidMode && isTokenValid) {
            log.info("Meta webhook verification handshake succeeded");
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(challenge);
        }

        log.warn("Meta webhook verification handshake failed: invalid mode or token");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody(required = false) byte[] rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        if (rawBody == null || rawBody.length == 0 || !signatureVerifier.isValid(rawBody, signature)) {
            log.warn("Invalid or missing webhook signature, payloadLength={}", rawBody != null ? rawBody.length : 0);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ingestService.ingest(rawBody, true);
        return ResponseEntity.ok().build();
    }
}
