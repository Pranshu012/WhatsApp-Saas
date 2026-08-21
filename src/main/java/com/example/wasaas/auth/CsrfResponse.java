package com.example.wasaas.auth;

public record CsrfResponse(
    String token,
    String headerName,
    String parameterName
) {}
