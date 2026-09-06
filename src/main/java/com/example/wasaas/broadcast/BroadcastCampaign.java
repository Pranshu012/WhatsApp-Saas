package com.example.wasaas.broadcast;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "broadcast_campaigns")
public class BroadcastCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "whatsapp_account_id")
    private UUID whatsappAccountId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType = TargetType.ALL_CONTACTS;

    @Column(name = "target_stage")
    private String targetStage;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "template_language", nullable = false)
    private String templateLanguage = "en_US";

    @Column(name = "template_id")
    private UUID templateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_params", columnDefinition = "jsonb")
    private String templateParams;

    @Column(name = "message_preview", columnDefinition = "TEXT")
    private String messagePreview;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BroadcastStatus status = BroadcastStatus.DRAFT;

    @Column(name = "total_recipients", nullable = false)
    private int totalRecipients = 0;

    @Column(name = "sent_count", nullable = false)
    private int sentCount = 0;

    @Column(name = "failed_count", nullable = false)
    private int failedCount = 0;

    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BroadcastCampaign() {}

    public BroadcastCampaign(UUID tenantId, UUID whatsappAccountId, String name,
                             TargetType targetType, String targetStage,
                             String templateName, String templateLanguage,
                             UUID templateId, String templateParams,
                             String messagePreview, Instant scheduledFor) {
        this.tenantId = tenantId;
        this.whatsappAccountId = whatsappAccountId;
        this.name = name;
        this.targetType = targetType != null ? targetType : TargetType.ALL_CONTACTS;
        this.targetStage = targetStage;
        this.templateName = templateName;
        this.templateLanguage = templateLanguage != null ? templateLanguage : "en_US";
        this.templateId = templateId;
        this.templateParams = templateParams;
        this.messagePreview = messagePreview;
        this.scheduledFor = scheduledFor;
        this.status = scheduledFor != null ? BroadcastStatus.SCHEDULED : BroadcastStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getWhatsappAccountId() { return whatsappAccountId; }
    public String getName() { return name; }
    public TargetType getTargetType() { return targetType; }
    public String getTargetStage() { return targetStage; }
    public String getTemplateName() { return templateName; }
    public String getTemplateLanguage() { return templateLanguage; }
    public UUID getTemplateId() { return templateId; }
    public String getTemplateParams() { return templateParams; }
    public String getMessagePreview() { return messagePreview; }
    public BroadcastStatus getStatus() { return status; }
    public int getTotalRecipients() { return totalRecipients; }
    public int getSentCount() { return sentCount; }
    public int getFailedCount() { return failedCount; }
    public Instant getScheduledFor() { return scheduledFor; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setWhatsappAccountId(UUID whatsappAccountId) { this.whatsappAccountId = whatsappAccountId; }
    public void setName(String name) { this.name = name; }
    public void setStatus(BroadcastStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    public void setTotalRecipients(int totalRecipients) { this.totalRecipients = totalRecipients; }
    public void setSentCount(int sentCount) { this.sentCount = sentCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setScheduledFor(Instant scheduledFor) { this.scheduledFor = scheduledFor; }
}
