package com.example.wasaas.whatsapp;

import com.example.wasaas.tenant.BaseTenantEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_accounts")
public class WhatsAppAccount extends BaseTenantEntity {

    @Id
    private UUID id;

    @Column(name = "waba_id", nullable = false)
    private String wabaId;

    @Column(name = "phone_number_id", nullable = false)
    private String phoneNumberId;

    @Column(name = "display_phone_number")
    private String displayPhoneNumber;

    @Column(name = "verified_name")
    private String verifiedName;

    @Column(name = "quality_rating", nullable = false)
    private String qualityRating;

    @Column(name = "messaging_limit_tier", nullable = false)
    private String messagingLimitTier;

    @JsonIgnore
    @Column(name = "access_token_encrypted", nullable = false)
    private byte[] accessTokenEncrypted;

    @Column(name = "token_encrypted_at", nullable = false)
    private Instant tokenEncryptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WhatsAppAccountStatus status;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WhatsAppAccount() {}

    public WhatsAppAccount(UUID tenantId,
                           String wabaId,
                           String phoneNumberId,
                           String displayPhoneNumber,
                           String verifiedName,
                           String qualityRating,
                           String messagingLimitTier,
                           byte[] accessTokenEncrypted) {
        this.id = UUID.randomUUID();
        setTenantId(tenantId);
        this.wabaId = wabaId;
        this.phoneNumberId = phoneNumberId;
        this.displayPhoneNumber = displayPhoneNumber;
        this.verifiedName = verifiedName;
        this.qualityRating = qualityRating != null ? qualityRating : "UNKNOWN";
        this.messagingLimitTier = messagingLimitTier != null ? messagingLimitTier : "TIER_250";
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.tokenEncryptedAt = Instant.now();
        this.status = WhatsAppAccountStatus.CONNECTED;
        this.connectedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (connectedAt == null) {
            connectedAt = now;
        }
        if (tokenEncryptedAt == null) {
            tokenEncryptedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getWabaId() { return wabaId; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public String getDisplayPhoneNumber() { return displayPhoneNumber; }
    public String getVerifiedName() { return verifiedName; }
    public String getQualityRating() { return qualityRating; }
    public String getMessagingLimitTier() { return messagingLimitTier; }
    public WhatsAppAccountStatus getStatus() { return status; }
    public Instant getConnectedAt() { return connectedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @JsonIgnore
    public byte[] getAccessTokenEncrypted() { return accessTokenEncrypted; }

    public void updateToken(byte[] newEncryptedToken) {
        this.accessTokenEncrypted = newEncryptedToken;
        this.tokenEncryptedAt = Instant.now();
        this.status = WhatsAppAccountStatus.CONNECTED;
    }

    public void updateDetails(String displayPhoneNumber, String verifiedName, String qualityRating, String messagingLimitTier) {
        this.displayPhoneNumber = displayPhoneNumber;
        this.verifiedName = verifiedName;
        if (qualityRating != null) this.qualityRating = qualityRating;
        if (messagingLimitTier != null) this.messagingLimitTier = messagingLimitTier;
    }

    public void disconnect() {
        this.status = WhatsAppAccountStatus.DISCONNECTED;
    }
}
