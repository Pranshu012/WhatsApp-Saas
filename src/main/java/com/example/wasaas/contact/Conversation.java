package com.example.wasaas.contact;

import com.example.wasaas.tenant.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "whatsapp_account_id")
    private UUID whatsappAccountId;

    @Column(name = "last_inbound_at")
    private Instant lastInboundAt;

    @Column(name = "last_outbound_at")
    private Instant lastOutboundAt;

    @Column(name = "service_window_expires_at")
    private Instant serviceWindowExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConversationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Conversation() {}

    public Conversation(UUID tenantId, UUID contactId, UUID whatsappAccountId, Instant inboundTimestamp) {
        setTenantId(tenantId);
        this.contactId = contactId;
        this.whatsappAccountId = whatsappAccountId;
        this.status = ConversationStatus.OPEN;
        refreshInbound(inboundTimestamp);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = ConversationStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void refreshInbound(Instant inboundTimestamp) {
        Instant ts = inboundTimestamp != null ? inboundTimestamp : Instant.now();
        this.lastInboundAt = ts;
        this.serviceWindowExpiresAt = ts.plus(24, ChronoUnit.HOURS);
        this.status = ConversationStatus.OPEN;
    }

    public void recordOutbound(Instant outboundTimestamp) {
        this.lastOutboundAt = outboundTimestamp != null ? outboundTimestamp : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getContactId() { return contactId; }
    public UUID getWhatsappAccountId() { return whatsappAccountId; }
    public Instant getLastInboundAt() { return lastInboundAt; }
    public Instant getLastOutboundAt() { return lastOutboundAt; }
    public Instant getServiceWindowExpiresAt() { return serviceWindowExpiresAt; }
    public ConversationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
