package com.example.wasaas.whatsapp.meta;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetaPhoneNumberDetails(
    String id,
    @JsonProperty("display_phone_number") String displayPhoneNumber,
    @JsonProperty("verified_name") String verifiedName,
    @JsonProperty("quality_rating") String qualityRating,
    @JsonProperty("messaging_limit_tier") String messagingLimitTier
) {}
