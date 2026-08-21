package com.example.wasaas.auth;

import java.util.UUID;

public record AuthUserResponse(
    UUID userId,
    UUID tenantId,
    String email,
    String fullName,
    String role
) {}
