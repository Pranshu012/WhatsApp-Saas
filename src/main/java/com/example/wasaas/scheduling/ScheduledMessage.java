package com.example.wasaas.scheduling;

import com.example.wasaas.common.exception.DomainException;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_messages")
public class ScheduledMessage extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "whatsapp_account_id", nullable = false)
    private UUID whatsappAccountId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", columnDefinition = "jsonb")
    private String variables;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "timezone", nullable = false)
    private String timezone = "Asia/Kolkata";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScheduledMessageStatus status = ScheduledMessageStatus.SCHEDULED;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScheduledMessage() {}

    public ScheduledMessage(UUID tenantId, UUID contactId, UUID templateId,
                            UUID whatsappAccountId, String variables,
                            Instant scheduledFor, String timezone) {
        setTenantId(tenantId);
        this.contactId = contactId;
        this.templateId = templateId;
        this.whatsappAccountId = whatsappAccountId;
        this.variables = variables;
        this.scheduledFor = scheduledFor;
        this.timezone = (timezone != null && !timezone.isBlank()) ? timezone : "Asia/Kolkata";
        this.status = ScheduledMessageStatus.SCHEDULED;
    }

    public void markEnqueued(UUID jobId) {
        if (this.status != ScheduledMessageStatus.SCHEDULED) {
            throw new DomainException(HttpStatus.CONFLICT, "Cannot enqueue scheduled message in status: " + this.status);
        }
        this.status = ScheduledMessageStatus.ENQUEUED;
        this.jobId = jobId;
    }

    public void cancel() {
        if (this.status != ScheduledMessageStatus.SCHEDULED) {
            throw new DomainException(HttpStatus.CONFLICT,
                    "Cannot cancel scheduled message in status: " + this.status + ". Only messages in SCHEDULED status can be cancelled.");
        }
        this.status = ScheduledMessageStatus.CANCELLED;
    }

    public void markSent() {
        this.status = ScheduledMessageStatus.SENT;
    }

    public void markFailed(String reason) {
        this.status = ScheduledMessageStatus.FAILED;
        this.failureReason = reason;
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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getContactId() { return contactId; }
    public UUID getTemplateId() { return templateId; }
    public UUID getWhatsappAccountId() { return whatsappAccountId; }
    public String getVariables() { return variables; }
    public Instant getScheduledFor() { return scheduledFor; }
    public String getTimezone() { return timezone; }
    public ScheduledMessageStatus getStatus() { return status; }
    public UUID getJobId() { return jobId; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
