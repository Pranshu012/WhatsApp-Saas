package com.example.wasaas.admin;

import java.util.Map;

public record AdminPlatformStatsDto(
        long totalTenants,
        long activeTenants,
        long trialingTenants,
        long suspendedTenants,
        long totalUsers,
        long totalMessagesThisMonth,
        long totalActiveWhatsAppAccounts,
        long estimatedMonthlyRevenueInr,
        Map<String, Long> planDistribution
) {}
