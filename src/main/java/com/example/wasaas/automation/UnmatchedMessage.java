package com.example.wasaas.automation;

import com.example.wasaas.tenant.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "unmatched_messages")
public class UnmatchedMessage extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "whatsapp_account_id")
    private UUID whatsappAccountId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "sender_phone", nullable = false)
    private String senderPhone;

    @Column(name = "message_text", nullable = false)
    private String messageText;

    @Column(name = "wamid")
    private String wamid;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected UnmatchedMessage() {}

    public UnmatchedMessage(UUID tenantId, UUID whatsappAccountId, UUID contactId,
                            String senderPhone, String messageText, String wamid) {
        setTenantId(tenantId);
        this.whatsappAccountId = whatsappAccountId;
        this.contactId = contactId;
        this.senderPhone = senderPhone;
        this.messageText = messageText != null ? messageText : "";
        this.wamid = wamid;
    }

    @PrePersist
    protected void onCreate() {
        if (this.receivedAt == null) {
            this.receivedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getWhatsappAccountId() { return whatsappAccountId; }
    public UUID getContactId() { return contactId; }
    public String getSenderPhone() { return senderPhone; }
    public String getMessageText() { return messageText; }
    public String getWamid() { return wamid; }
    public Instant getReceivedAt() { return receivedAt; }
}
