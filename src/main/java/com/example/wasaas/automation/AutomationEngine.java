package com.example.wasaas.automation;

import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.client.TemplateComponent;
import com.example.wasaas.whatsapp.inbound.InboundMessageReceivedEvent;
import com.example.wasaas.whatsapp.send.MessagingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class AutomationEngine {

    private static final Logger log = LoggerFactory.getLogger(AutomationEngine.class);

    private final AutomationRuleRepository ruleRepository;
    private final UnmatchedMessageRepository unmatchedMessageRepository;
    private final RuleMatcher ruleMatcher;
    private final AutoReplyRateLimiter rateLimiter;
    private final com.example.wasaas.automation.faq.FaqMatchService faqMatchService;
    private final MessagingService messagingService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public AutomationEngine(AutomationRuleRepository ruleRepository,
                            UnmatchedMessageRepository unmatchedMessageRepository,
                            RuleMatcher ruleMatcher,
                            AutoReplyRateLimiter rateLimiter,
                            com.example.wasaas.automation.faq.FaqMatchService faqMatchService,
                            MessagingService messagingService,
                            ApplicationEventPublisher eventPublisher,
                            ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.unmatchedMessageRepository = unmatchedMessageRepository;
        this.ruleMatcher = ruleMatcher;
        this.rateLimiter = rateLimiter;
        this.faqMatchService = faqMatchService;
        this.messagingService = messagingService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @EventListener
    @Transactional
    public void onInboundMessage(InboundMessageReceivedEvent event) {
        if (event == null || event.text() == null || event.text().isBlank()) {
            return;
        }

        UUID tenantId = event.tenantId();
        TenantContext.set(tenantId);

        try {
            List<AutomationRule> rules = ruleRepository.findAllByTenantIdAndEnabledTrueOrderByPriorityAsc(tenantId);
            boolean matched = false;

            for (AutomationRule rule : rules) {
                if (ruleMatcher.matches(rule, event.text())) {
                    matched = true;
                    log.info("Rule [{}] (id={}) matched for incoming message from [{}] under tenant [{}]",
                            rule.getName(), rule.getId(), event.fromE164(), tenantId);

                    // Check per-contact rate limiter to prevent reply loops
                    if (!rateLimiter.tryAcquire(tenantId, event.fromE164())) {
                        log.warn("Auto-reply suppressed for contact [{}] due to rate limit under tenant [{}]",
                                event.fromE164(), tenantId);
                        break; // Stop processing further rules
                    }

                    executeAction(rule, event);
                    break; // First match wins
                }
            }

            if (!matched) {
                // Fallback 1: FAQ Match via PostgreSQL FTS + pg_trgm (F14)
                com.example.wasaas.automation.faq.FaqMatchResult faqResult = faqMatchService.findMatch(tenantId, event.text());

                if (faqResult.isConfident()) {
                    log.info("FAQ [{}] matched query [{}] with confidence score [{}] for contact [{}]",
                            faqResult.question(), event.text(), faqResult.confidenceScore(), event.fromE164());

                    if (rateLimiter.tryAcquire(tenantId, event.fromE164())) {
                        String idempotencyKey = "faq:" + event.wamid() + ":" + faqResult.id();
                        messagingService.sendText(
                                event.whatsappAccountId(),
                                event.fromE164(),
                                faqResult.answer(),
                                idempotencyKey
                        );
                    } else {
                        log.warn("FAQ auto-reply suppressed for contact [{}] due to rate limit under tenant [{}]",
                                event.fromE164(), tenantId);
                    }
                    return;
                }

                // Fallback 2: Unmatched / Escalation (ADR-007 dataset)
                log.info("No rule or confident FAQ match for message [{}] from [{}] (bestScore={}), escalating",
                        event.wamid(), event.fromE164(), faqResult.confidenceScore());

                UnmatchedMessage unmatched = new UnmatchedMessage(
                        tenantId,
                        event.whatsappAccountId(),
                        event.contactId(),
                        event.fromE164(),
                        event.text(),
                        event.wamid()
                );
                unmatchedMessageRepository.save(unmatched);

                eventPublisher.publishEvent(new UnmatchedMessageEvent(
                        tenantId,
                        event.whatsappAccountId(),
                        event.contactId(),
                        event.fromE164(),
                        event.text(),
                        event.wamid(),
                        Instant.now()
                ));
            }

        } finally {
            TenantContext.clear();
        }
    }

    private void executeAction(AutomationRule rule, InboundMessageReceivedEvent event) {
        try {
            JsonNode payloadNode = objectMapper.readTree(rule.getActionPayload());

            switch (rule.getActionType()) {
                case SEND_TEXT -> {
                    String replyText = payloadNode.has("text") ? payloadNode.get("text").asText() : "";
                    String idempotencyKey = "auto:" + event.wamid() + ":" + rule.getId();
                    messagingService.sendText(
                            event.whatsappAccountId(),
                            event.fromE164(),
                            replyText,
                            idempotencyKey
                    );
                }
                case SEND_TEMPLATE -> {
                    String templateName = payloadNode.has("templateName") ? payloadNode.get("templateName").asText() : "";
                    String language = payloadNode.has("language") ? payloadNode.get("language").asText() : "en_US";
                    List<TemplateComponent> components = List.of();
                    if (payloadNode.has("components")) {
                        components = objectMapper.convertValue(payloadNode.get("components"), new TypeReference<List<TemplateComponent>>() {});
                    }
                    String idempotencyKey = "auto:" + event.wamid() + ":" + rule.getId();
                    messagingService.sendTemplate(
                            event.whatsappAccountId(),
                            event.fromE164(),
                            templateName,
                            language,
                            components,
                            BillingCategory.UTILITY,
                            idempotencyKey
                    );
                }
                case ESCALATE, SEND_INTERACTIVE -> {
                    log.info("Action type [{}] recorded for rule [{}] on message [{}]",
                            rule.getActionType(), rule.getName(), event.wamid());
                }
            }
        } catch (Exception e) {
            log.error("Failed to execute action for rule [{}] on message [{}]: {}",
                    rule.getName(), event.wamid(), e.getMessage(), e);
        }
    }
}
