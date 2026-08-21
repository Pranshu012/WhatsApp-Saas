package com.example.wasaas.scheduling;

import com.example.wasaas.whatsapp.client.TemplateComponent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScheduleMessageCommand(
    UUID contactId,
    UUID templateId,
    UUID whatsappAccountId,
    List<TemplateComponent> components,
    Instant scheduledFor,
    String timezone
) {}
