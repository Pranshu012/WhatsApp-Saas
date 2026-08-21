package com.example.wasaas.whatsapp.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MetaSendMessageResponse(
    @JsonProperty("messaging_product") String messagingProduct,
    List<MetaContact> contacts,
    List<MetaMessage> messages
) {
    public record MetaContact(String input, @JsonProperty("wa_id") String waId) {}
    public record MetaMessage(String id) {}
}
