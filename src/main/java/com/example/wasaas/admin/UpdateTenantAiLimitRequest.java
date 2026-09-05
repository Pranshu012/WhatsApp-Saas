package com.example.wasaas.admin;

import jakarta.validation.constraints.Min;

public record UpdateTenantAiLimitRequest(
        @Min(value = 10, message = "Monthly limit must be at least 10")
        int monthlyLimit
) {}
