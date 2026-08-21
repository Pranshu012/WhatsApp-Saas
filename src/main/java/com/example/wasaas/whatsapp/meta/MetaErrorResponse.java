package com.example.wasaas.whatsapp.meta;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetaErrorResponse(
    MetaError error
) {
    public record MetaError(
        String message,
        String type,
        int code,
        @JsonProperty("error_subcode") Integer errorSubcode,
        @JsonProperty("fbtrace_id") String fbtraceId
    ) {}
}
