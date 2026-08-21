package com.example.wasaas.whatsapp.webhook;

import com.example.wasaas.job.JobService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class WebhookIngestService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestService.class);
    public static final String JOB_TYPE = "PROCESS_WEBHOOK_EVENT";

    private final WebhookEventRepository webhookEventRepository;
    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public WebhookIngestService(WebhookEventRepository webhookEventRepository,
                                JobService jobService,
                                ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ingest(byte[] rawBody, boolean signatureValid) {
        String payloadString = new String(rawBody, StandardCharsets.UTF_8);
        String eventId = null;
        String wabaId = null;
        String phoneNumberId = null;

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (root != null && root.has("entry") && root.get("entry").isArray() && !root.get("entry").isEmpty()) {
                JsonNode firstEntry = root.get("entry").get(0);
                if (firstEntry.has("id")) {
                    wabaId = firstEntry.get("id").asText();
                }

                if (firstEntry.has("changes") && firstEntry.get("changes").isArray() && !firstEntry.get("changes").isEmpty()) {
                    JsonNode changeValue = firstEntry.get("changes").get(0).get("value");
                    if (changeValue != null) {
                        if (changeValue.has("metadata") && changeValue.get("metadata").has("phone_number_id")) {
                            phoneNumberId = changeValue.get("metadata").get("phone_number_id").asText();
                        }

                        if (changeValue.has("messages") && changeValue.get("messages").isArray() && !changeValue.get("messages").isEmpty()) {
                            JsonNode msg = changeValue.get("messages").get(0);
                            if (msg.has("id")) {
                                eventId = msg.get("id").asText();
                            }
                        } else if (changeValue.has("statuses") && changeValue.get("statuses").isArray() && !changeValue.get("statuses").isEmpty()) {
                            JsonNode status = changeValue.get("statuses").get(0);
                            if (status.has("id")) {
                                String statusName = status.has("status") ? status.get("status").asText() : "update";
                                eventId = status.get("id").asText() + ":" + statusName;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse shallow webhook payload metadata: {}", e.getMessage());
        }

        // Deduplication check
        if (eventId != null && !eventId.isBlank()) {
            Optional<WebhookEvent> existing = webhookEventRepository.findByEventId(eventId);
            if (existing.isPresent()) {
                log.info("Duplicate webhook event ignored: {}", eventId);
                return;
            }
        }

        WebhookEvent event = new WebhookEvent(eventId, wabaId, phoneNumberId, payloadString, signatureValid);
        try {
            event = webhookEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate webhook event ignored due to database constraint: {}", eventId);
            return;
        }

        // Enqueue background processing job with idempotent key
        String idempotencyKey = "wh:" + (eventId != null ? eventId : event.getId().toString());
        String jobPayload = "{\"webhookEventId\":\"" + event.getId() + "\"}";
        jobService.enqueue(null, JOB_TYPE, jobPayload, idempotencyKey);

        log.debug("Ingested webhook event ID [{}] for WABA [{}], Phone Number ID [{}]",
                event.getId(), wabaId, phoneNumberId);
    }
}
