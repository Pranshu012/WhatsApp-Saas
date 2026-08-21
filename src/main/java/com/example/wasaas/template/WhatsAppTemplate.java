package com.example.wasaas.template;

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

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "whatsapp_templates")
public class WhatsAppTemplate extends BaseTenantEntity {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\d+)\\}\\}");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "whatsapp_account_id", nullable = false)
    private UUID whatsappAccountId;

    @Column(name = "meta_template_id")
    private String metaTemplateId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "language", nullable = false)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_category")
    private TemplateCategory requestedCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private TemplateCategory category;

    @Column(name = "category_conflict", nullable = false)
    private boolean categoryConflict;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TemplateStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "body_text", nullable = false)
    private String bodyText;

    @Column(name = "header_type")
    private String headerType;

    @Column(name = "variable_count", nullable = false)
    private int variableCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "components", columnDefinition = "jsonb")
    private String components;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WhatsAppTemplate() {}

    public WhatsAppTemplate(UUID tenantId, UUID whatsappAccountId, String metaTemplateId,
                            String name, String language, TemplateCategory requestedCategory,
                            TemplateCategory category, TemplateStatus status, String rejectionReason,
                            String bodyText, String headerType, String components) {
        setTenantId(tenantId);
        this.whatsappAccountId = whatsappAccountId;
        this.metaTemplateId = metaTemplateId;
        this.name = name;
        this.language = language;
        this.requestedCategory = requestedCategory;
        this.category = category;
        this.status = status != null ? status : TemplateStatus.PENDING;
        this.rejectionReason = rejectionReason;
        this.bodyText = bodyText != null ? bodyText : "";
        this.headerType = headerType;
        this.components = components;
        this.variableCount = calculateVariableCount(this.bodyText);
        this.categoryConflict = evaluateCategoryConflict(requestedCategory, category);
    }

    public static int calculateVariableCount(String text) {
        if (text == null || text.isBlank()) return 0;
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        int max = 0;
        while (matcher.find()) {
            try {
                int num = Integer.parseInt(matcher.group(1));
                if (num > max) {
                    max = num;
                }
            } catch (NumberFormatException ignored) {}
        }
        return max;
    }

    private static boolean evaluateCategoryConflict(TemplateCategory requested, TemplateCategory assigned) {
        return requested != null && assigned != null && requested != assigned;
    }

    public void updateFromMeta(String metaTemplateId, TemplateCategory assignedCategory,
                               TemplateStatus status, String rejectionReason, String bodyText,
                               String headerType, String components, Instant syncedAt) {
        this.metaTemplateId = metaTemplateId;
        this.category = assignedCategory;
        this.categoryConflict = evaluateCategoryConflict(this.requestedCategory, assignedCategory);
        this.status = status;
        this.rejectionReason = rejectionReason;
        if (bodyText != null && !bodyText.isBlank()) {
            this.bodyText = bodyText;
            this.variableCount = calculateVariableCount(bodyText);
        }
        this.headerType = headerType;
        this.components = components;
        this.syncedAt = syncedAt != null ? syncedAt : Instant.now();
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
    public UUID getWhatsappAccountId() { return whatsappAccountId; }
    public String getMetaTemplateId() { return metaTemplateId; }
    public String getName() { return name; }
    public String getLanguage() { return language; }
    public TemplateCategory getRequestedCategory() { return requestedCategory; }
    public TemplateCategory getCategory() { return category; }
    public boolean isCategoryConflict() { return categoryConflict; }
    public TemplateStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public String getBodyText() { return bodyText; }
    public String getHeaderType() { return headerType; }
    public int getVariableCount() { return variableCount; }
    public String getComponents() { return components; }
    public Instant getSyncedAt() { return syncedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(TemplateStatus status) { this.status = status; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
