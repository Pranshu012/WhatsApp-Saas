package com.example.wasaas.ledger;

import java.util.UUID;

public record RecordOutboundIntentCommand(
    UUID whatsappAccountId,
    String recipientPhoneNumber,
    BillingCategory billingCategory,
    String templateName,
    ConversationWindow conversationWindow,
    String idempotencyKey,
    UUID jobId
) {}
