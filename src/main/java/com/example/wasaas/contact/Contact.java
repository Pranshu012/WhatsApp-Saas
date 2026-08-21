package com.example.wasaas.contact;

import com.example.wasaas.tenant.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contacts")
public class Contact extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_e164", nullable = false)
    private String phoneE164;

    @Column(name = "phone_hash", nullable = false)
    private String phoneHash;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "opt_in_status", nullable = false)
    private String optInStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Contact() {}

    public Contact(UUID tenantId, String phoneE164, String phoneHash, String displayName) {
        setTenantId(tenantId);
        this.phoneE164 = phoneE164;
        this.phoneHash = phoneHash;
        this.displayName = displayName;
        this.optInStatus = "OPTED_IN";
        this.lastSeenAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.lastSeenAt == null) {
            this.lastSeenAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getPhoneE164() { return phoneE164; }
    public String getPhoneHash() { return phoneHash; }
    public String getDisplayName() { return displayName; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public String getOptInStatus() { return optInStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateActivity(String displayName, Instant seenAt) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
        if (seenAt != null) {
            this.lastSeenAt = seenAt;
        } else {
            this.lastSeenAt = Instant.now();
        }
    }
}
