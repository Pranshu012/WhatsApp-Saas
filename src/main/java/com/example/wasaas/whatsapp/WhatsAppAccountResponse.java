package com.example.wasaas.whatsapp;

import java.time.Instant;
import java.util.UUID;

public record WhatsAppAccountResponse(
    UUID id,
    String wabaId,
    String phoneNumberId,
    String displayPhoneNumber,
    String verifiedName,
    String qualityRating,
    String messagingLimitTier,
    boolean paymentMethodAttached,
    String status,
    Instant connectedAt
) {
    public static WhatsAppAccountResponse from(WhatsAppAccount account) {
        return new WhatsAppAccountResponse(
                account.getId(),
                account.getWabaId(),
                account.getPhoneNumberId(),
                account.getDisplayPhoneNumber(),
                account.getVerifiedName(),
                account.getQualityRating(),
                account.getMessagingLimitTier(),
                account.isPaymentMethodAttached(),
                account.getStatus().name(),
                account.getConnectedAt()
        );
    }
}
