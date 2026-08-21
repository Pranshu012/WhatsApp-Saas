package com.example.wasaas.whatsapp.meta;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetaTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType
) {}
