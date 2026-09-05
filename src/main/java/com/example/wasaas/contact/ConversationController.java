package com.example.wasaas.contact;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.ledger.MessageLedger;
import com.example.wasaas.ledger.MessageLedgerRepository;
import com.example.wasaas.ledger.PhonePrivacyUtils;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.send.MessagingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final ContactRepository contactRepository;
    private final MessageLedgerRepository ledgerRepository;
    private final MessagingService messagingService;
    private final ConversationMessageRepository conversationMessageRepository;

    public ConversationController(ConversationRepository conversationRepository,
                                  ContactRepository contactRepository,
                                  MessageLedgerRepository ledgerRepository,
                                  MessagingService messagingService,
                                  ConversationMessageRepository conversationMessageRepository) {
        this.conversationRepository = conversationRepository;
        this.contactRepository = contactRepository;
        this.ledgerRepository = ledgerRepository;
        this.messagingService = messagingService;
        this.conversationMessageRepository = conversationMessageRepository;
    }

    @GetMapping
    public ResponseEntity<List<ConversationSummaryDto>> listConversations() {
        UUID tenantId = TenantContext.require();
        List<Conversation> conversations = conversationRepository.findAllByTenantId(tenantId);
        List<ConversationSummaryDto> dtos = new ArrayList<>();

        for (Conversation conv : conversations) {
            Contact contact = contactRepository.findById(conv.getContactId()).orElse(null);

            boolean windowActive = conv.getServiceWindowExpiresAt() != null &&
                    conv.getServiceWindowExpiresAt().isAfter(Instant.now());

            List<ConversationMessage> recentMsgs = conversationMessageRepository.findTop8ByConversationIdOrderByCreatedAtDesc(conv.getId());
            String lastMessageText = null;
            String lastMessageSender = null;
            Instant lastMessageAt = null;
            if (recentMsgs != null && !recentMsgs.isEmpty()) {
                ConversationMessage latest = recentMsgs.get(0);
                lastMessageText = latest.getTextContent();
                lastMessageSender = latest.getSenderType();
                lastMessageAt = latest.getCreatedAt();
            }

            dtos.add(new ConversationSummaryDto(
                    conv.getId(),
                    conv.getContactId(),
                    contact != null ? contact.getDisplayName() : null,
                    contact != null ? contact.getPhoneE164() : null,
                    conv.getLastInboundAt(),
                    conv.getLastOutboundAt(),
                    conv.getStatus(),
                    windowActive,
                    conv.getServiceWindowExpiresAt(),
                    lastMessageText,
                    lastMessageSender,
                    lastMessageAt
            ));
        }

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ChatMessageDto>> getConversationMessages(@PathVariable UUID id) {
        UUID tenantId = TenantContext.require();
        Conversation conv = conversationRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Conversation not found: " + id));

        List<ConversationMessage> convMsgs = conversationMessageRepository.findAllByConversationIdOrderByCreatedAtAsc(id);
        if (convMsgs != null && !convMsgs.isEmpty()) {
            List<ChatMessageDto> dtos = convMsgs.stream()
                    .map(m -> new ChatMessageDto(
                            m.getId(),
                            m.getSenderType(),
                            m.getTextContent(),
                            m.getWamid(),
                            m.getCreatedAt(),
                            "DELIVERED"
                    ))
                    .toList();
            return ResponseEntity.ok(dtos);
        }

        // Fallback for legacy messages without text
        Contact contact = contactRepository.findById(conv.getContactId()).orElse(null);
        if (contact != null) {
            List<MessageLedger> legacyLedger = ledgerRepository.findAllByTenantIdAndRecipientPhoneHashOrderByCreatedAtAsc(
                    tenantId, contact.getPhoneHash()
            );
            List<ChatMessageDto> legacyDtos = legacyLedger.stream()
                    .map(l -> new ChatMessageDto(
                            l.getId(),
                            l.getDirection() == com.example.wasaas.ledger.MessageDirection.INBOUND ? "CUSTOMER" : "AI_BOT",
                            l.getTemplateName() != null ? "[Template: " + l.getTemplateName() + "]" : "WhatsApp Message",
                            l.getWamid(),
                            l.getCreatedAt(),
                            l.getStatus() != null ? l.getStatus().name() : "SENT"
                    ))
                    .toList();
            return ResponseEntity.ok(legacyDtos);
        }

        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<Void> reply(@PathVariable UUID id, @Valid @RequestBody ReplyRequest request) {
        UUID tenantId = TenantContext.require();
        Conversation conv = conversationRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Conversation not found: " + id));

        Contact contact = contactRepository.findById(conv.getContactId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Contact not found for conversation"));

        if (conv.getServiceWindowExpiresAt() == null || !conv.getServiceWindowExpiresAt().isAfter(Instant.now())) {
            throw new DomainException(HttpStatus.CONFLICT,
                    "This conversation's 24-hour service window has closed. Send an approved WhatsApp template instead.");
        }

        long windowBucket = conv.getServiceWindowExpiresAt().toEpochMilli();
        String stableContent = conv.getId() + ":" + request.text().trim() + ":" + windowBucket;
        String contentHash = PhonePrivacyUtils.hashPhoneNumber(stableContent).substring(0, 16);
        
        String idempotencyKey = (request.clientRequestId() != null && !request.clientRequestId().isBlank())
                ? "manual-reply:" + conv.getId() + ":" + request.clientRequestId().trim()
                : "manual-reply:" + conv.getId() + ":" + contentHash;

        messagingService.sendText(
                conv.getWhatsappAccountId(),
                contact.getPhoneE164(),
                request.text(),
                idempotencyKey
        );

        try {
            ConversationMessage agentMsg = new ConversationMessage(
                    tenantId,
                    conv.getId(),
                    contact.getId(),
                    "AGENT",
                    request.text().trim(),
                    null
            );
            conversationMessageRepository.save(agentMsg);
        } catch (Exception e) {
            // Non-blocking log
        }

        return ResponseEntity.accepted().build();
    }

    public record ConversationSummaryDto(
            UUID id,
            UUID contactId,
            String contactName,
            String phoneE164,
            Instant lastInboundAt,
            Instant lastOutboundAt,
            ConversationStatus status,
            boolean serviceWindowActive,
            Instant serviceWindowExpiresAt,
            String lastMessageText,
            String lastMessageSender,
            Instant lastMessageAt
    ) {}

    public record ChatMessageDto(
            UUID id,
            String senderType,
            String textContent,
            String wamid,
            Instant createdAt,
            String status
    ) {}

    public record ReplyRequest(
            @NotBlank String text,
            String clientRequestId
    ) {}
}
