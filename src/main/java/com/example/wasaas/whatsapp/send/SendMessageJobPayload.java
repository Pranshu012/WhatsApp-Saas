package com.example.wasaas.whatsapp.send;

import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.whatsapp.client.ReplyButton;
import com.example.wasaas.whatsapp.client.TemplateComponent;

import java.util.List;
import java.util.UUID;

public record SendMessageJobPayload(
    UUID accountId,
    String toE164,
    String type, // "TEXT", "TEMPLATE", or "INTERACTIVE"
    String text,
    String templateName,
    String languageCode,
    List<TemplateComponent> components,
    BillingCategory billingCategory,
    String callerIdempotencyKey,
    List<ReplyButton> buttons
) {
    public SendMessageJobPayload(
            UUID accountId,
            String toE164,
            String type,
            String text,
            String templateName,
            String languageCode,
            List<TemplateComponent> components,
            BillingCategory billingCategory,
            String callerIdempotencyKey
    ) {
        this(accountId, toE164, type, text, templateName, languageCode, components, billingCategory, callerIdempotencyKey, null);
    }
}
