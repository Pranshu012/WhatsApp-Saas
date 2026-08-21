package com.example.wasaas.ledger;

import com.example.wasaas.tenant.BaseTenantEntity;
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
@Table(name = "message_ledger_status_events")
public class MessageLedgerStatusEvent extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ledger_id", nullable = false, updatable = false)
    private UUID ledgerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false)
    private MessageLedgerStatus status;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload")
    private String rawPayload;

    protected MessageLedgerStatusEvent() {}

    public MessageLedgerStatusEvent(UUID ledgerId, MessageLedgerStatus status, Instant occurredAt, String rawPayload) {
        this.ledgerId = ledgerId;
        this.status = status;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.rawPayload = rawPayload;
    }

    @PrePersist
    protected void onCreate() {
        if (this.occurredAt == null) {
            this.occurredAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getLedgerId() { return ledgerId; }
    public MessageLedgerStatus getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getRawPayload() { return rawPayload; }
}
