package com.example.wasaas.whatsapp.send;

import com.example.wasaas.job.JobService;
import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.ledger.PhonePrivacyUtils;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.client.TemplateComponent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * MessagingService — the ONLY public send entry point.
 * Enqueues messages to the durable PostgreSQL background job queue.
 * Direct synchronous HTTP calls to Meta Cloud API outside background jobs are prohibited.
 */
@Service
public class MessagingService {

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public MessagingService(JobService jobService, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    public void sendText(UUID accountId, String toE164, String text, String callerIdempotencyKey) {
        UUID tenantId = TenantContext.require();

        SendMessageJobPayload payload = new SendMessageJobPayload(
                accountId,
                toE164,
                "TEXT",
                text,
                null,
                null,
                null,
                BillingCategory.SERVICE,
                callerIdempotencyKey
        );

        enqueueJob(tenantId, payload, accountId, toE164, callerIdempotencyKey);
    }

    public void sendTemplate(UUID accountId, String toE164, String templateName,
                             String languageCode, List<TemplateComponent> components,
                             BillingCategory category, String callerIdempotencyKey) {
        UUID tenantId = TenantContext.require();

        SendMessageJobPayload payload = new SendMessageJobPayload(
                accountId,
                toE164,
                "TEMPLATE",
                null,
                templateName,
                languageCode != null ? languageCode : "en_US",
                components,
                category != null ? category : BillingCategory.MARKETING,
                callerIdempotencyKey
        );

        enqueueJob(tenantId, payload, accountId, toE164, callerIdempotencyKey);
    }

    private void enqueueJob(UUID tenantId, SendMessageJobPayload payload,
                            UUID accountId, String toE164, String callerIdempotencyKey) {
        String jobKey = null;
        if (callerIdempotencyKey != null && !callerIdempotencyKey.isBlank()) {
            jobKey = "send:" + accountId + ":" + PhonePrivacyUtils.normalize(toE164) + ":" + callerIdempotencyKey;
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            jobService.enqueue(tenantId, SendMessageJobHandler.JOB_TYPE, payloadJson, jobKey);
            log.debug("Enqueued WhatsApp send job for account [{}], recipient [{}]", accountId, PhonePrivacyUtils.extractLast4(toE164));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize send job payload", e);
        }
    }
}
