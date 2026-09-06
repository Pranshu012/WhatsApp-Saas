package com.example.wasaas.auth.oauth;

import jakarta.validation.constraints.NotBlank;

public record GoogleOAuthRequest(
        @NotBlank(message = "Credential or ID token is required")
        String credential,
        String businessName,
        String email,
        String name
) {}
