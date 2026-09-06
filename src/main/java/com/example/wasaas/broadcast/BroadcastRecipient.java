package com.example.wasaas.broadcast;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "broadcast_recipients")
public class BroadcastRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "broadcast_id", nullable = false)
    private UUID broadcastId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "phone_e164", nullable = false)
    private String phoneE164;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "custom_params", columnDefinition = "JSONB")
    private String customParams;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecipientStatus status = RecipientStatus.PENDING;

    @Column(name = "wamid")
    private String wamid;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BroadcastRecipient() {}

    public BroadcastRecipient(UUID broadcastId, UUID tenantId, UUID contactId,
                              String phoneE164, String contactName, String customParams) {
        this.broadcastId = broadcastId;
        this.tenantId = tenantId;
        this.contactId = contactId;
        this.phoneE164 = phoneE164;
        this.contactName = contactName;
        this.customParams = customParams;
        this.status = RecipientStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBroadcastId() { return broadcastId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getContactId() { return contactId; }
    public String getPhoneE164() { return phoneE164; }
    public String getContactName() { return contactName; }
    public String getCustomParams() { return customParams; }
    public RecipientStatus getStatus() { return status; }
    public String getWamid() { return wamid; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(RecipientStatus status) { this.status = status; }
    public void setWamid(String wamid) { this.wamid = wamid; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
