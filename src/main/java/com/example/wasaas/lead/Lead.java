package com.example.wasaas.lead;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private LeadStage stage = LeadStage.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature", nullable = false)
    private LeadTemperature temperature = LeadTemperature.WARM;

    @Column(name = "score", nullable = false)
    private int score = 50;

    @Column(name = "deal_value", nullable = false)
    private BigDecimal dealValue = BigDecimal.ZERO;

    @Column(name = "requirement_summary", columnDefinition = "TEXT")
    private String requirementSummary;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "last_interaction_at", nullable = false)
    private Instant lastInteractionAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Lead() {}

    public Lead(UUID tenantId, UUID contactId) {
        this.tenantId = tenantId;
        this.contactId = contactId;
        this.stage = LeadStage.NEW;
        this.temperature = LeadTemperature.WARM;
        this.score = 50;
        this.dealValue = BigDecimal.ZERO;
        this.lastInteractionAt = Instant.now();
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

    public UUID getContactId() {
        return contactId;
    }

    public void setContactId(UUID contactId) {
        this.contactId = contactId;
    }

    public LeadStage getStage() {
        return stage;
    }

    public void setStage(LeadStage stage) {
        this.stage = stage;
        this.updatedAt = Instant.now();
    }

    public LeadTemperature getTemperature() {
        return temperature;
    }

    public void setTemperature(LeadTemperature temperature) {
        this.temperature = temperature;
        this.updatedAt = Instant.now();
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = Math.max(0, Math.min(100, score));
        if (this.score >= 70) {
            this.temperature = LeadTemperature.HOT;
        } else if (this.score >= 40) {
            this.temperature = LeadTemperature.WARM;
        } else {
            this.temperature = LeadTemperature.COLD;
        }
        this.updatedAt = Instant.now();
    }

    public BigDecimal getDealValue() {
        return dealValue;
    }

    public void setDealValue(BigDecimal dealValue) {
        this.dealValue = dealValue != null ? dealValue : BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    public String getRequirementSummary() {
        return requirementSummary;
    }

    public void setRequirementSummary(String requirementSummary) {
        this.requirementSummary = requirementSummary;
        this.updatedAt = Instant.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void setLastInteractionAt(Instant lastInteractionAt) {
        this.lastInteractionAt = lastInteractionAt;
        this.updatedAt = Instant.now();
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
