package com.example.wasaas.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminTenantDto(
        UUID tenantId,
        String businessName,
        String slug,
        String status,
        String timezone,
        String gstin,
        String legalName,
        String billingAddress,
        Instant createdAt,
        // Owner user details
        UUID ownerId,
        String ownerName,
        String ownerEmail,
        // WhatsApp status
        boolean whatsAppConnected,
        String displayPhoneNumber,
        String qualityRating,
        String messagingLimitTier,
        // Subscription details
        UUID subscriptionId,
        String planType,
        String subscriptionStatus,
        Instant trialStartDate,
        Instant trialExpiresAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        long daysRemaining,
        boolean isSubscriptionValid,
        int monthlyPricePaise,
        String notes,
        // Usage stats
        long totalMessagesThisMonth,
        long totalFaqs,
        long totalAutomationRules
) {}
