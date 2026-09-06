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

/**
 * Asynchronous job handler for processing raw Meta WhatsApp webhook payloads.
 * <p>
 * Core responsibilities:
 * <ul>
 *   <li><b>Idempotent Ingestion:</b> Deduplicates messages using Meta {@code wamid} against the {@code message_ledger}.</li>
 *   <li><b>Delivery Tracking:</b> Correlates status updates (SENT, DELIVERED, READ, FAILED) with outbound ledger records.</li>
 *   <li><b>Compliance Engine:</b> Detects STOP/UNSUBSCRIBE keywords, flips {@code opt_in_status} to OPTED_OUT,
 *       dispatches instant confirmation, and suppresses bot automated replies.</li>
 *   <li><b>CRM Integration:</b> Automatically creates or updates leads and tracks last message timestamps.</li>
 * </ul>
 */
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
    private final com.example.wasaas.contact.ConversationMessageRepository conversationMessageRepository;
    private final com.example.wasaas.lead.LeadService leadService;
    private final com.example.wasaas.whatsapp.send.MessagingService messagingService;

    public ProcessWebhookEventHandler(WebhookEventRepository webhookEventRepository,
                                      WhatsAppAccountRepository accountRepository,
                                      ContactRepository contactRepository,
                                      ConversationRepository conversationRepository,
                                      LedgerService ledgerService,
                                      WhatsAppTemplateRepository templateRepository,
                                      ApplicationEventPublisher eventPublisher,
                                      ObjectMapper objectMapper,
                                      com.example.wasaas.contact.ConversationMessageRepository conversationMessageRepository,
                                      com.example.wasaas.lead.LeadService leadService,
                                      com.example.wasaas.whatsapp.send.MessagingService messagingService) {
        this.webhookEventRepository = webhookEventRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.conversationRepository = conversationRepository;
        this.ledgerService = ledgerService;
        this.templateRepository = templateRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.conversationMessageRepository = conversationMessageRepository;
        this.leadService = leadService;
        this.messagingService = messagingService;
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
            if (msg.has("type")) {
                String type = msg.get("type").asText();
                if ("text".equals(type) && msg.has("text")) {
                    text = msg.get("text").has("body") ? msg.get("text").get("body").asText() : null;
                } else if ("interactive".equals(type) && msg.has("interactive")) {
                    JsonNode interactiveNode = msg.get("interactive");
                    if (interactiveNode.has("button_reply")) {
                        JsonNode btn = interactiveNode.get("button_reply");
                        text = btn.has("title") ? btn.get("title").asText() : (btn.has("id") ? btn.get("id").asText() : null);
                    } else if (interactiveNode.has("list_reply")) {
                        JsonNode listReply = interactiveNode.get("list_reply");
                        text = listReply.has("title") ? listReply.get("title").asText() : (listReply.has("id") ? listReply.get("id").asText() : null);
                    }
                } else if ("button".equals(type) && msg.has("button")) {
                    JsonNode btn = msg.get("button");
                    text = btn.has("text") ? btn.get("text").asText() : (btn.has("payload") ? btn.get("payload").asText() : null);
                }
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

            // 3.5 Persist Conversation Message for Multi-Turn AI Context & Update Lead CRM
            if (text != null && !text.isBlank()) {
                try {
                    com.example.wasaas.contact.ConversationMessage custMsg = new com.example.wasaas.contact.ConversationMessage(
                            account.getTenantId(),
                            savedConversation.getId(),
                            savedContact.getId(),
                            "CUSTOMER",
                            text.trim(),
                            wamid
                    );
                    conversationMessageRepository.save(custMsg);
                    leadService.processCustomerMessage(account.getTenantId(), savedContact.getId(), text.trim());
                } catch (Exception e) {
                    log.warn("Failed to persist conversation message/lead for tenant [{}]: {}", account.getTenantId(), e.getMessage());
                }
            }

            // 3.6 Automated Compliance: Keyword-based Opt-Out (STOP) and Opt-In (START)
            if (isOptOutKeyword(text)) {
                savedContact.setOptInStatus("OPTED_OUT");
                contactRepository.save(savedContact);
                log.info("Contact [{}] opted OUT under tenant [{}] via keyword [{}]",
                        PhonePrivacyUtils.extractLast4(fromE164), account.getTenantId(), text);

                String ackText = "You have been successfully unsubscribed from promotional messages. Reply START anytime to subscribe again.";
                try {
                    messagingService.sendText(account.getId(), fromE164, ackText, "optout:" + (wamid != null ? wamid : System.currentTimeMillis()));
                    com.example.wasaas.contact.ConversationMessage ackMsg = new com.example.wasaas.contact.ConversationMessage(
                            account.getTenantId(),
                            savedConversation.getId(),
                            savedContact.getId(),
                            "AI_BOT",
                            ackText,
                            null
                    );
                    conversationMessageRepository.save(ackMsg);
                } catch (Exception e) {
                    log.warn("Failed to send opt-out confirmation to [{}]: {}", PhonePrivacyUtils.extractLast4(fromE164), e.getMessage());
                }
                continue; // Suppress further bot/rule automation
            } else if (isOptInKeyword(text)) {
                savedContact.setOptInStatus("OPTED_IN");
                contactRepository.save(savedContact);
                log.info("Contact [{}] opted IN under tenant [{}] via keyword [{}]",
                        PhonePrivacyUtils.extractLast4(fromE164), account.getTenantId(), text);

                String ackText = "Welcome back! You have successfully resubscribed to receive promotional updates.";
                try {
                    messagingService.sendText(account.getId(), fromE164, ackText, "optin:" + (wamid != null ? wamid : System.currentTimeMillis()));
                    com.example.wasaas.contact.ConversationMessage ackMsg = new com.example.wasaas.contact.ConversationMessage(
                            account.getTenantId(),
                            savedConversation.getId(),
                            savedContact.getId(),
                            "AI_BOT",
                            ackText,
                            null
                    );
                    conversationMessageRepository.save(ackMsg);
                } catch (Exception e) {
                    log.warn("Failed to send opt-in confirmation to [{}]: {}", PhonePrivacyUtils.extractLast4(fromE164), e.getMessage());
                }
                continue; // Suppress further bot/rule automation
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

    private boolean isOptOutKeyword(String text) {
        if (text == null) return false;
        String clean = text.trim().toLowerCase();
        return clean.equals("stop")
                || clean.equals("unsubscribe")
                || clean.equals("cancel")
                || clean.equals("opt out")
                || clean.equals("optout")
                || clean.equals("opt-out")
                || clean.equals("stop promotions")
                || clean.equals("stop promo")
                || clean.equals("roko")
                || clean.equals("band karo")
                || clean.equals("halt")
                || clean.equals("quit");
    }

    private boolean isOptInKeyword(String text) {
        if (text == null) return false;
        String clean = text.trim().toLowerCase();
        return clean.equals("start")
                || clean.equals("subscribe")
                || clean.equals("unstop")
                || clean.equals("shuru karo");
    }
}
