package com.example.wasaas.subscription;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionDto(
        UUID id,
        UUID tenantId,
        String planType,
        String status,
        Instant trialStartDate,
        Instant trialExpiresAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        int monthlyPricePaise,
        String currency,
        long daysRemaining,
        boolean isCurrentlyValid,
        String notes
) {
    public static SubscriptionDto fromEntity(Subscription sub) {
        return new SubscriptionDto(
                sub.getId(),
                sub.getTenantId(),
                sub.getPlanType().name(),
                sub.getStatus().name(),
                sub.getTrialStartDate(),
                sub.getTrialExpiresAt(),
                sub.getCurrentPeriodStart(),
                sub.getCurrentPeriodEnd(),
                sub.getMonthlyPricePaise(),
                sub.getCurrency(),
                sub.getDaysRemaining(),
                sub.isCurrentlyValid(),
                sub.getNotes()
        );
    }
}
