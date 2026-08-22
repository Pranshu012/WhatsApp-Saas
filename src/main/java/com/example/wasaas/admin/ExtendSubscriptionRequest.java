package com.example.wasaas.admin;

import jakarta.validation.constraints.Min;

public record ExtendSubscriptionRequest(
        @Min(value = 1, message = "Extra days must be at least 1")
        int extraDays,

        String notes
) {}
