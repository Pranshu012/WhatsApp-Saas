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
    private final com.example.wasaas.subscription.SubscriptionService subscriptionService;
    private final com.example.wasaas.ai.AiAssistantService aiAssistantService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final com.example.wasaas.contact.ConversationMessageRepository conversationMessageRepository;

    public AutomationEngine(AutomationRuleRepository ruleRepository,
                            UnmatchedMessageRepository unmatchedMessageRepository,
                            RuleMatcher ruleMatcher,
                            AutoReplyRateLimiter rateLimiter,
                            com.example.wasaas.automation.faq.FaqMatchService faqMatchService,
                            MessagingService messagingService,
                            com.example.wasaas.subscription.SubscriptionService subscriptionService,
                            com.example.wasaas.ai.AiAssistantService aiAssistantService,
                            ApplicationEventPublisher eventPublisher,
                            ObjectMapper objectMapper,
                            com.example.wasaas.contact.ConversationMessageRepository conversationMessageRepository) {
        this.ruleRepository = ruleRepository;
        this.unmatchedMessageRepository = unmatchedMessageRepository;
        this.ruleMatcher = ruleMatcher;
        this.rateLimiter = rateLimiter;
        this.faqMatchService = faqMatchService;
        this.messagingService = messagingService;
        this.subscriptionService = subscriptionService;
        this.aiAssistantService = aiAssistantService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.conversationMessageRepository = conversationMessageRepository;
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
            // Check if tenant's subscription/trial is valid before sending automated replies
            if (!subscriptionService.isSubscriptionValid(tenantId)) {
                log.warn("Auto-reply suppressed: Tenant [{}] subscription/trial is not valid or suspended", tenantId);
                return;
            }

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
                        try {
                            com.example.wasaas.contact.ConversationMessage faqMsg =
                                    new com.example.wasaas.contact.ConversationMessage(
                                            tenantId,
                                            event.conversationId(),
                                            event.contactId(),
                                            "AI_BOT",
                                            faqResult.answer().trim(),
                                            null
                                    );
                            conversationMessageRepository.save(faqMsg);
                        } catch (Exception e) {
                            log.warn("Failed to persist FAQ conversation message: {}", e.getMessage());
                        }
                    } else {
                        log.warn("FAQ auto-reply suppressed for contact [{}] due to rate limit under tenant [{}]",
                                event.fromE164(), tenantId);
                    }
                    return;
                }

                // Fallback 2: Smart Gemini AI Assistant (with multi-turn conversation memory)
                try {
                    java.util.Optional<String> aiReplyOpt = aiAssistantService.generateReply(tenantId, event.conversationId(), event.text());
                    if (aiReplyOpt.isPresent()) {
                        String aiReply = aiReplyOpt.get();
                        log.info("Gemini AI generated reply for contact [{}] under tenant [{}]: {}", event.fromE164(), tenantId, aiReply);

                        if (rateLimiter.tryAcquire(tenantId, event.fromE164())) {
                            String idempotencyKey = "ai:" + event.wamid();
                            messagingService.sendText(
                                    event.whatsappAccountId(),
                                    event.fromE164(),
                                    aiReply,
                                    idempotencyKey
                            );
                            try {
                                com.example.wasaas.contact.ConversationMessage botMsg =
                                        new com.example.wasaas.contact.ConversationMessage(
                                                tenantId,
                                                event.conversationId(),
                                                event.contactId(),
                                                "AI_BOT",
                                                aiReply.trim(),
                                                null
                                        );
                                conversationMessageRepository.save(botMsg);
                            } catch (Exception e) {
                                log.warn("Failed to persist AI conversation message: {}", e.getMessage());
                            }
                            return;
                        } else {
                            log.warn("AI auto-reply suppressed for contact [{}] due to rate limit under tenant [{}]",
                                    event.fromE164(), tenantId);
                            return;
                        }
                    }
                } catch (Exception e) {
                    log.error("AI assistant processing error for tenant [{}]: {}", tenantId, e.getMessage());
                }

                // Fallback 3: Unmatched / Escalation for human review
                log.info("No rule, FAQ, or AI match for message [{}] from [{}], logging as Unmatched",
                        event.wamid(), event.fromE164());

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
                case SEND_INTERACTIVE -> {
                    String bodyText = payloadNode.has("text") ? payloadNode.get("text").asText() : "";
                    List<com.example.wasaas.whatsapp.client.ReplyButton> buttons = new java.util.ArrayList<>();
                    if (payloadNode.has("buttons") && payloadNode.get("buttons").isArray()) {
                        for (JsonNode btnNode : payloadNode.get("buttons")) {
                            String id = btnNode.has("id") ? btnNode.get("id").asText() : "btn_" + System.currentTimeMillis();
                            String title = btnNode.has("title") ? btnNode.get("title").asText() : "Option";
                            buttons.add(new com.example.wasaas.whatsapp.client.ReplyButton(id, title));
                        }
                    }
                    String idempotencyKey = "auto:" + event.wamid() + ":" + rule.getId();
                    messagingService.sendInteractiveButtons(
                            event.whatsappAccountId(),
                            event.fromE164(),
                            bodyText,
                            buttons,
                            idempotencyKey
                    );
                }
                case ESCALATE -> {
                    log.info("Action type ESCALATE recorded for rule [{}] on message [{}]",
                            rule.getName(), event.wamid());
                }
            }
        } catch (Exception e) {
            log.error("Failed to execute action for rule [{}] on message [{}]: {}",
                    rule.getName(), event.wamid(), e.getMessage(), e);
        }
    }
}
