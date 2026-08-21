package com.example.wasaas.whatsapp.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "waba_id")
    private String wabaId;

    @Column(name = "phone_number_id")
    private String phoneNumberId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false)
    private String rawPayload;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WebhookEventStatus status;

    protected WebhookEvent() {}

    public WebhookEvent(String eventId, String wabaId, String phoneNumberId, String rawPayload, boolean signatureValid) {
        this.eventId = eventId;
        this.wabaId = wabaId;
        this.phoneNumberId = phoneNumberId;
        this.rawPayload = rawPayload;
        this.signatureValid = signatureValid;
        this.status = WebhookEventStatus.PENDING;
        this.receivedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.receivedAt == null) {
            this.receivedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = WebhookEventStatus.PENDING;
        }
    }

    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public String getWabaId() { return wabaId; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public String getRawPayload() { return rawPayload; }
    public boolean isSignatureValid() { return signatureValid; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public WebhookEventStatus getStatus() { return status; }

    public void markProcessed() {
        this.status = WebhookEventStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void markIgnored() {
        this.status = WebhookEventStatus.IGNORED;
        this.processedAt = Instant.now();
    }

    public void markFailed() {
        this.status = WebhookEventStatus.FAILED;
        this.processedAt = Instant.now();
    }
}
