package com.example.wasaas.lead;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lead_qualification_rules")
public class LeadQualificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "match_keywords", nullable = false, columnDefinition = "TEXT")
    private String matchKeywords;

    @Column(name = "score_boost", nullable = false)
    private int scoreBoost = 20;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public LeadQualificationRule() {}

    public LeadQualificationRule(UUID tenantId, String ruleName, String matchKeywords, int scoreBoost) {
        this.tenantId = tenantId;
        this.ruleName = ruleName;
        this.matchKeywords = matchKeywords;
        this.scoreBoost = scoreBoost;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getMatchKeywords() {
        return matchKeywords;
    }

    public void setMatchKeywords(String matchKeywords) {
        this.matchKeywords = matchKeywords;
    }

    public int getScoreBoost() {
        return scoreBoost;
    }

    public void setScoreBoost(int scoreBoost) {
        this.scoreBoost = scoreBoost;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
