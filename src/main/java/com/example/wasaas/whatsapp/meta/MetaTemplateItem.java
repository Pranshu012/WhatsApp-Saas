package com.example.wasaas.whatsapp.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record MetaTemplateItem(
    String id,
    String name,
    String language,
    String status,
    String category,
    @JsonProperty("rejection_reason") String rejectionReason,
    List<JsonNode> components
) {}
