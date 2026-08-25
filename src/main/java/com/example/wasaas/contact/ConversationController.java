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

    public ConversationController(ConversationRepository conversationRepository,
                                  ContactRepository contactRepository,
                                  MessageLedgerRepository ledgerRepository,
                                  MessagingService messagingService) {
        this.conversationRepository = conversationRepository;
        this.contactRepository = contactRepository;
        this.ledgerRepository = ledgerRepository;
        this.messagingService = messagingService;
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

            dtos.add(new ConversationSummaryDto(
                    conv.getId(),
                    conv.getContactId(),
                    contact != null ? contact.getDisplayName() : null,
                    contact != null ? contact.getPhoneE164() : null,
                    conv.getLastInboundAt(),
                    conv.getLastOutboundAt(),
                    conv.getStatus(),
                    windowActive,
                    conv.getServiceWindowExpiresAt()
            ));
        }

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageLedger>> getConversationMessages(@PathVariable UUID id) {
        UUID tenantId = TenantContext.require();
        Conversation conv = conversationRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Conversation not found: " + id));

        Contact contact = contactRepository.findById(conv.getContactId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Contact not found for conversation"));

        List<MessageLedger> messages = ledgerRepository.findAllByTenantIdAndRecipientPhoneHashOrderByCreatedAtAsc(
                tenantId, contact.getPhoneHash()
        );

        return ResponseEntity.ok(messages);
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
            Instant serviceWindowExpiresAt
    ) {}

    public record ReplyRequest(
            @NotBlank String text,
            String clientRequestId
    ) {}
}
