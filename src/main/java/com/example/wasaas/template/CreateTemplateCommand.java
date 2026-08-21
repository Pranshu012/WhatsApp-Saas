package com.example.wasaas.template;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

public record CreateTemplateCommand(
    UUID whatsappAccountId,
    String name,
    String language,
    TemplateCategory category,
    String bodyText,
    List<JsonNode> components
) {}
