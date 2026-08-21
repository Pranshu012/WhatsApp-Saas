package com.example.wasaas.whatsapp.inbound;

import java.time.Instant;
import java.util.UUID;

public record InboundMessageReceivedEvent(
    UUID tenantId,
    UUID contactId,
    UUID conversationId,
    UUID whatsappAccountId,
    String wamid,
    String fromE164,
    String text,
    Instant timestamp,
    Instant serviceWindowExpiresAt
) {}
