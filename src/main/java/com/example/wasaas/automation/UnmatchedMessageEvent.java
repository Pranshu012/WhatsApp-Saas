package com.example.wasaas.automation;

import java.time.Instant;
import java.util.UUID;

public record UnmatchedMessageEvent(
    UUID tenantId,
    UUID whatsappAccountId,
    UUID contactId,
    String senderPhone,
    String messageText,
    String wamid,
    Instant timestamp
) {}
