package com.example.wasaas.whatsapp;

import jakarta.validation.constraints.NotBlank;

public record ConnectWhatsAppRequest(
    @NotBlank(message = "Meta authorization code is required")
    String code,

    @NotBlank(message = "WABA ID is required")
    String wabaId,

    @NotBlank(message = "Phone Number ID is required")
    String phoneNumberId
) {}
