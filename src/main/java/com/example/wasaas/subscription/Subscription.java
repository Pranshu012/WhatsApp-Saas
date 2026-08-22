package com.example.wasaas.subscription;

import com.example.wasaas.tenant.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription extends BaseTenantEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "trial_start_date")
    private Instant trialStartDate;

    @Column(name = "trial_expires_at")
    private Instant trialExpiresAt;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "monthly_price_paise", nullable = false)
    private int monthlyPricePaise = 49900;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Subscription() {
    }

    public Subscription(UUID tenantId, PlanType planType, SubscriptionStatus status,
                        Instant trialStartDate, Instant trialExpiresAt,
                        Instant currentPeriodStart, Instant currentPeriodEnd,
                        int monthlyPricePaise, String currency) {
        setTenantId(tenantId);
        this.id = UUID.randomUUID();
        this.planType = planType;
        this.status = status;
        this.trialStartDate = trialStartDate;
        this.trialExpiresAt = trialExpiresAt;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.monthlyPricePaise = monthlyPricePaise;
        this.currency = currency != null ? currency : "INR";
    }

    public static Subscription create14DayTrial(UUID tenantId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(14, ChronoUnit.DAYS);
        return new Subscription(
                tenantId,
                PlanType.FREE_TRIAL,
                SubscriptionStatus.TRIALING,
                now,
                expiresAt,
                now,
                expiresAt,
                49900,
                "INR"
        );
    }

    public static Subscription createActive(UUID tenantId, PlanType planType, int durationDays) {
        Instant now = Instant.now();
        Instant periodEnd = now.plus(durationDays, ChronoUnit.DAYS);
        return new Subscription(
                tenantId,
                planType,
                SubscriptionStatus.ACTIVE,
                null,
                null,
                now,
                periodEnd,
                49900,
                "INR"
        );
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isCurrentlyValid() {
        Instant now = Instant.now();
        if (status == SubscriptionStatus.SUSPENDED || status == SubscriptionStatus.CANCELLED || status == SubscriptionStatus.EXPIRED) {
            return false;
        }
        if (status == SubscriptionStatus.TRIALING) {
            return trialExpiresAt != null && trialExpiresAt.isAfter(now);
        }
        if (status == SubscriptionStatus.ACTIVE) {
            return currentPeriodEnd == null || currentPeriodEnd.isAfter(now);
        }
        return false;
    }

    public long getDaysRemaining() {
        Instant now = Instant.now();
        Instant target = (status == SubscriptionStatus.TRIALING) ? trialExpiresAt : currentPeriodEnd;
        if (target == null || target.isBefore(now)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(now, target);
    }

    // Getters and setters
    public UUID getId() { return id; }
    public PlanType getPlanType() { return planType; }
    public void setPlanType(PlanType planType) { this.planType = planType; }
    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }
    public Instant getTrialStartDate() { return trialStartDate; }
    public void setTrialStartDate(Instant trialStartDate) { this.trialStartDate = trialStartDate; }
    public Instant getTrialExpiresAt() { return trialExpiresAt; }
    public void setTrialExpiresAt(Instant trialExpiresAt) { this.trialExpiresAt = trialExpiresAt; }
    public Instant getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(Instant currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(Instant currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
    public int getMonthlyPricePaise() { return monthlyPricePaise; }
    public void setMonthlyPricePaise(int monthlyPricePaise) { this.monthlyPricePaise = monthlyPricePaise; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
