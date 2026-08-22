package com.example.wasaas.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActivateTenantRequest(
        @NotNull(message = "Plan type is required")
        String planType,

        @Min(value = 1, message = "Duration must be at least 1 day")
        int durationDays,

        String notes
) {}
