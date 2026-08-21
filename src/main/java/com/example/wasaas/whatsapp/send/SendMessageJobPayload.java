package com.example.wasaas.whatsapp.send;

import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.whatsapp.client.TemplateComponent;

import java.util.List;
import java.util.UUID;

public record SendMessageJobPayload(
    UUID accountId,
    String toE164,
    String type, // "TEXT" or "TEMPLATE"
    String text,
    String templateName,
    String languageCode,
    List<TemplateComponent> components,
    BillingCategory billingCategory,
    String callerIdempotencyKey
) {}
