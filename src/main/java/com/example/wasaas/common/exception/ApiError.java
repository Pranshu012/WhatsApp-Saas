package com.example.wasaas.common.exception;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message, String requestId) { }
