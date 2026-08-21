package com.example.wasaas.whatsapp.meta;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetaWabaDetails(
    String id,
    String name,
    @JsonProperty("timezone_id") String timezoneId,
    @JsonProperty("message_template_namespace") String messageTemplateNamespace
) {}
