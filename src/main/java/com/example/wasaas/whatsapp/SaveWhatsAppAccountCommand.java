package com.example.wasaas.whatsapp;

import jakarta.validation.constraints.NotBlank;

public record SaveWhatsAppAccountCommand(
    @NotBlank(message = "WABA ID is required")
    String wabaId,

    @NotBlank(message = "Phone Number ID is required")
    String phoneNumberId,

    String displayPhoneNumber,
    String verifiedName,
    String qualityRating,
    String messagingLimitTier,

    @NotBlank(message = "Access token is required")
    String rawAccessToken
) {}
