package com.example.wasaas.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_ai_configs")
public class TenantAiConfig {

    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are a warm, helpful, and concise WhatsApp receptionist for this business. Reply in 1-2 sentences. " +
            "Match the customer's language (Hindi, English, or Hinglish). Never make up fake prices, discounts, or medical advice " +
            "not provided in the business context. If unsure, politely ask the customer to speak with a human or visit the store.";

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "api_key")
    private String apiKey;

    @Column(nullable = false)
    private String model = "gemini-flash-latest";

    @Column(name = "business_context", nullable = false, columnDefinition = "TEXT")
    private String businessContext = "";

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    @Column(name = "monthly_limit", nullable = false)
    private int monthlyLimit = 500;

    @Column(name = "used_this_month", nullable = false)
    private int usedThisMonth = 0;

    @Column(name = "current_period_month", nullable = false)
    private String currentPeriodMonth = java.time.YearMonth.now().toString();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public TenantAiConfig() {}

    public TenantAiConfig(UUID tenantId) {
        this.tenantId = tenantId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-flash-latest";
    }

    public String getBusinessContext() {
        return businessContext;
    }

    public void setBusinessContext(String businessContext) {
        this.businessContext = businessContext != null ? businessContext.trim() : "";
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt != null && !systemPrompt.isBlank() ? systemPrompt.trim() : DEFAULT_SYSTEM_PROMPT;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(int monthlyLimit) {
        this.monthlyLimit = Math.max(0, monthlyLimit);
    }

    public int getUsedThisMonth() {
        return usedThisMonth;
    }

    public void setUsedThisMonth(int usedThisMonth) {
        this.usedThisMonth = usedThisMonth;
    }

    public String getCurrentPeriodMonth() {
        return currentPeriodMonth;
    }

    public void setCurrentPeriodMonth(String currentPeriodMonth) {
        this.currentPeriodMonth = currentPeriodMonth;
    }

    public void incrementUsage() {
        this.usedThisMonth++;
    }
}
