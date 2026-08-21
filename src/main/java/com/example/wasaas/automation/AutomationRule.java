package com.example.wasaas.automation;

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

@Entity
@Table(name = "automation_rules")
public class AutomationRule extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private MatchType matchType;

    @Column(name = "match_value", nullable = false)
    private String matchValue;

    @Column(name = "case_sensitive", nullable = false)
    private boolean caseSensitive;

    @Column(name = "priority", nullable = false)
    private int priority = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_payload", columnDefinition = "jsonb", nullable = false)
    private String actionPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AutomationRule() {}

    public AutomationRule(UUID tenantId, String name, boolean enabled, MatchType matchType,
                          String matchValue, boolean caseSensitive, int priority,
                          ActionType actionType, String actionPayload) {
        setTenantId(tenantId);
        this.name = name;
        this.enabled = enabled;
        this.matchType = matchType;
        this.matchValue = matchValue;
        this.caseSensitive = caseSensitive;
        this.priority = priority;
        this.actionType = actionType;
        this.actionPayload = actionPayload;
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
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public MatchType getMatchType() { return matchType; }
    public String getMatchValue() { return matchValue; }
    public boolean isCaseSensitive() { return caseSensitive; }
    public int getPriority() { return priority; }
    public ActionType getActionType() { return actionType; }
    public String getActionPayload() { return actionPayload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setMatchValue(String matchValue) { this.matchValue = matchValue; }
    public void setActionPayload(String actionPayload) { this.actionPayload = actionPayload; }
}
