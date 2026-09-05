package com.example.wasaas.contact;

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
@Table(name = "conversation_messages")
public class ConversationMessage extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "sender_type", nullable = false)
    private String senderType; // 'CUSTOMER', 'AI_BOT', 'AGENT'

    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "wamid")
    private String wamid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConversationMessage() {}

    public ConversationMessage(UUID tenantId, UUID conversationId, UUID contactId,
                               String senderType, String textContent, String wamid) {
        setTenantId(tenantId);
        this.conversationId = conversationId;
        this.contactId = contactId;
        this.senderType = senderType;
        this.textContent = textContent;
        this.wamid = wamid;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public UUID getContactId() { return contactId; }
    public String getSenderType() { return senderType; }
    public String getTextContent() { return textContent; }
    public String getWamid() { return wamid; }
    public Instant getCreatedAt() { return createdAt; }
}
