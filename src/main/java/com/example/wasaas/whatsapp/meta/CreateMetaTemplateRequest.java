package com.example.wasaas.whatsapp.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record CreateMetaTemplateRequest(
    String name,
    String category,
    @JsonProperty("allow_category_change") boolean allowCategoryChange,
    String language,
    List<JsonNode> components
) {}
