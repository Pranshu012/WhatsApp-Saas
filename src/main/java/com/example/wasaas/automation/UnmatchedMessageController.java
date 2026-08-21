package com.example.wasaas.automation;

import com.example.wasaas.tenant.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/unmatched-messages")
public class UnmatchedMessageController {

    private final UnmatchedMessageRepository repository;

    public UnmatchedMessageController(UnmatchedMessageRepository repository) {
        this.repository = repository;
    }

    public record UnmatchedMessageResponse(
            UUID id,
            UUID whatsappAccountId,
            UUID contactId,
            String senderPhone,
            String messageText,
            String wamid,
            Instant receivedAt
    ) {
        public static UnmatchedMessageResponse from(UnmatchedMessage msg) {
            return new UnmatchedMessageResponse(
                    msg.getId(),
                    msg.getWhatsappAccountId(),
                    msg.getContactId(),
                    msg.getSenderPhone(),
                    msg.getMessageText(),
                    msg.getWamid(),
                    msg.getReceivedAt()
            );
        }
    }

    @GetMapping
    public List<UnmatchedMessageResponse> listUnmatchedMessages() {
        UUID tenantId = TenantContext.require();
        return repository.findAllByTenantIdOrderByReceivedAtDesc(tenantId).stream()
                .map(UnmatchedMessageResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnmatchedMessage(@PathVariable UUID id) {
        UUID tenantId = TenantContext.require();
        repository.findById(id).ifPresent(msg -> {
            if (tenantId.equals(msg.getTenantId())) {
                repository.delete(msg);
            }
        });
        return ResponseEntity.noContent().build();
    }
}
