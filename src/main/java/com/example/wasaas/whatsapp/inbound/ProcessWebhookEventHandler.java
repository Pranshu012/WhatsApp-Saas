package com.example.wasaas.whatsapp.inbound;

import com.example.wasaas.contact.Contact;
import com.example.wasaas.contact.ContactRepository;
import com.example.wasaas.contact.Conversation;
import com.example.wasaas.contact.ConversationRepository;
import com.example.wasaas.job.Job;
import com.example.wasaas.job.JobHandler;
import com.example.wasaas.job.PermanentJobException;
import com.example.wasaas.ledger.LedgerService;
import com.example.wasaas.ledger.MessageLedgerStatus;
import com.example.wasaas.ledger.PhonePrivacyUtils;
import com.example.wasaas.template.WhatsAppTemplateRepository;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountRepository;
import com.example.wasaas.whatsapp.webhook.WebhookEvent;
import com.example.wasaas.whatsapp.webhook.WebhookEventRepository;
import com.example.wasaas.whatsapp.webhook.WebhookIngestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class ProcessWebhookEventHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(ProcessWebhookEventHandler.class);

    private final WebhookEventRepository webhookEventRepository;
    private final WhatsAppAccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final ConversationRepository conversationRepository;
    private final LedgerService ledgerService;
    private final WhatsAppTemplateRepository templateRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public ProcessWebhookEventHandler(WebhookEventRepository webhookEventRepository,
                                      WhatsAppAccountRepository accountRepository,
                                      ContactRepository contactRepository,
                                      ConversationRepository conversationRepository,
                                      LedgerService ledgerService,
                                      WhatsAppTemplateRepository templateRepository,
                                      ApplicationEventPublisher eventPublisher,
                                      ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.conversationRepository = conversationRepository;
        this.ledgerService = ledgerService;
        this.templateRepository = templateRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public String jobType() {
        return WebhookIngestService.JOB_TYPE;
    }

    @Override
    public void handle(Job job) throws Exception {
        JsonNode payloadNode = objectMapper.readTree(job.getPayload());
        if (!payloadNode.has("webhookEventId")) {
            throw new PermanentJobException("Malformed job payload missing webhookEventId");
        }

        UUID webhookEventId = UUID.fromString(payloadNode.get("webhookEventId").asText());
        WebhookEvent event = webhookEventRepository.findById(webhookEventId)
                .orElseThrow(() -> new PermanentJobException("WebhookEvent not found for ID: " + webhookEventId));

        WhatsAppAccount account = null;
        if (event.getPhoneNumberId() != null && !event.getPhoneNumberId().isBlank()) {
            account = accountRepository.findByPhoneNumberId(event.getPhoneNumberId()).orElse(null);
        } else if (event.getWabaId() != null && !event.getWabaId().isBlank()) {
            account = accountRepository.findByWabaId(event.getWabaId()).orElse(null);
        }

        if (account == null) {
            log.warn("Unknown account for webhook event [{}] (phone={}, waba={})", webhookEventId, event.getPhoneNumberId(), event.getWabaId());
            event.markIgnored();
            webhookEventRepository.save(event);
            return;
        }

        UUID tenantId = account.getTenantId();
        TenantContext.set(tenantId);

        try {
            JsonNode root = objectMapper.readTree(event.getRawPayload());
            JsonNode valueNode = extractValueNode(root);

            if (valueNode == null) {
                log.info("No value change node found in webhook event [{}], marking IGNORED", webhookEventId);
                event.markIgnored();
                webhookEventRepository.save(event);
                return;
            }

            if (valueNode.has("messages") && valueNode.get("messages").isArray() && !valueNode.get("messages").isEmpty()) {
                processInboundMessages(valueNode, account, event);
                event.markProcessed();
            } else if (valueNode.has("statuses") && valueNode.get("statuses").isArray() && !valueNode.get("statuses").isEmpty()) {
                processStatusUpdates(valueNode, event);
                event.markProcessed();
            } else if (valueNode.has("message_template_id") || valueNode.has("message_template_name")) {
                processTemplateStatusUpdate(valueNode, account);
                event.markProcessed();
            } else {
                log.info("Unsupported webhook event shape in event [{}], marking IGNORED", webhookEventId);
                event.markIgnored();
            }

            webhookEventRepository.save(event);

        } finally {
            TenantContext.clear();
        }
    }

    private void processTemplateStatusUpdate(JsonNode valueNode, WhatsAppAccount account) {
        String metaTemplateId = valueNode.has("message_template_id") ? valueNode.get("message_template_id").asText() : null;
        String templateName = valueNode.has("message_template_name") ? valueNode.get("message_template_name").asText() : null;
        String language = valueNode.has("message_template_language") ? valueNode.get("message_template_language").asText() : null;
        String eventStatus = valueNode.has("event") ? valueNode.get("event").asText() : null;
        String reason = valueNode.has("reason") ? valueNode.get("reason").asText() : null;

        java.util.Optional<com.example.wasaas.template.WhatsAppTemplate> templateOpt = java.util.Optional.empty();
        if (metaTemplateId != null) {
            templateOpt = templateRepository.findByTenantIdAndMetaTemplateId(account.getTenantId(), metaTemplateId);
        }
        if (templateOpt.isEmpty() && templateName != null && language != null) {
            templateOpt = templateRepository.findByTenantIdAndNameAndLanguage(account.getTenantId(), templateName, language);
        }

        if (templateOpt.isPresent()) {
            com.example.wasaas.template.WhatsAppTemplate template = templateOpt.get();
            if (eventStatus != null) {
                try {
                    template.setStatus(com.example.wasaas.template.TemplateStatus.valueOf(eventStatus.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
            if (reason != null) {
                template.setRejectionReason(reason);
            }
            templateRepository.save(template);
            log.info("Updated template [{}] status to [{}] via webhook for tenant [{}]",
                    template.getName(), template.getStatus(), account.getTenantId());
        }
    }

    private void processInboundMessages(JsonNode valueNode, WhatsAppAccount account, WebhookEvent event) {
        JsonNode messages = valueNode.get("messages");
        String profileName = null;
        if (valueNode.has("contacts") && valueNode.get("contacts").isArray() && !valueNode.get("contacts").isEmpty()) {
            JsonNode contactNode = valueNode.get("contacts").get(0);
            if (contactNode.has("profile") && contactNode.get("profile").has("name")) {
                profileName = contactNode.get("profile").get("name").asText();
            }
        }

        for (JsonNode msg : messages) {
            String wamid = msg.has("id") ? msg.get("id").asText() : null;
            String from = msg.has("from") ? msg.get("from").asText() : null;
            if (from == null) continue;

            String fromE164 = from.startsWith("+") ? from : "+" + from;
            Instant timestamp = msg.has("timestamp")
                    ? Instant.ofEpochSecond(msg.get("timestamp").asLong())
                    : Instant.now();

            String text = null;
            if (msg.has("type") && "text".equals(msg.get("type").asText()) && msg.has("text")) {
                text = msg.get("text").has("body") ? msg.get("text").get("body").asText() : null;
            }

            // 1. Upsert Contact (stores full E.164 phone number)
            String phoneHash = PhonePrivacyUtils.hashPhoneNumber(fromE164);
            Contact contact = contactRepository.findByTenantIdAndPhoneE164(account.getTenantId(), fromE164)
                    .orElseGet(() -> new Contact(account.getTenantId(), fromE164, phoneHash, null));
            contact.updateActivity(profileName, timestamp);
            final Contact savedContact = contactRepository.save(contact);

            // 2. Upsert/Refresh Conversation (sets 24-hour service window expiry)
            Conversation conversation = conversationRepository
                    .findByTenantIdAndContactIdAndWhatsappAccountId(account.getTenantId(), savedContact.getId(), account.getId())
                    .orElseGet(() -> new Conversation(account.getTenantId(), savedContact.getId(), account.getId(), timestamp));
            conversation.refreshInbound(timestamp);
            final Conversation savedConversation = conversationRepository.save(conversation);

            // 3. Record INBOUND_FREE on Message Ledger (stores only hash + last4)
            if (wamid != null) {
                ledgerService.recordInboundMessage(account.getId(), fromE164, wamid, timestamp);
            }

            // 4. Publish Spring Domain Event
            eventPublisher.publishEvent(new InboundMessageReceivedEvent(
                    account.getTenantId(),
                    savedContact.getId(),
                    savedConversation.getId(),
                    account.getId(),
                    wamid,
                    fromE164,
                    text,
                    timestamp,
                    savedConversation.getServiceWindowExpiresAt()
            ));

            log.info("Processed inbound message [{}] from [{}] for tenant [{}] (window expires at [{}])",
                    wamid, PhonePrivacyUtils.extractLast4(fromE164), account.getTenantId(), conversation.getServiceWindowExpiresAt());
        }
    }

    private void processStatusUpdates(JsonNode valueNode, WebhookEvent event) {
        JsonNode statuses = valueNode.get("statuses");
        for (JsonNode statusNode : statuses) {
            String wamid = statusNode.has("id") ? statusNode.get("id").asText() : null;
            String statusStr = statusNode.has("status") ? statusNode.get("status").asText() : "";
            if (wamid == null) continue;

            MessageLedgerStatus mappedStatus = switch (statusStr.toLowerCase()) {
                case "sent" -> MessageLedgerStatus.SENT;
                case "delivered" -> MessageLedgerStatus.DELIVERED;
                case "read" -> MessageLedgerStatus.READ;
                case "failed" -> MessageLedgerStatus.FAILED;
                default -> MessageLedgerStatus.SENT;
            };

            ledgerService.recordStatusEvent(wamid, mappedStatus, event.getRawPayload());
        }
    }

    private JsonNode extractValueNode(JsonNode root) {
        if (root.has("entry") && root.get("entry").isArray() && !root.get("entry").isEmpty()) {
            JsonNode firstEntry = root.get("entry").get(0);
            if (firstEntry.has("changes") && firstEntry.get("changes").isArray() && !firstEntry.get("changes").isEmpty()) {
                return firstEntry.get("changes").get(0).get("value");
            }
        }
        return null;
    }
}
