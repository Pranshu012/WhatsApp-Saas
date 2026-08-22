package com.example.wasaas.tenant;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
public class Tenant {
    @Id private UUID id;
    @Column(name = "business_name", nullable = false) private String businessName;
    @Column(nullable = false, unique = true) private String slug;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TenantStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @Column(name = "timezone", nullable = false) private String timezone = "Asia/Kolkata";
    @Column(name = "gstin") private String gstin;
    @Column(name = "legal_name") private String legalName;
    @Column(name = "billing_address") private String billingAddress;

    protected Tenant() { }
    private Tenant(String businessName, String slug) {
        this.id = UUID.randomUUID();
        this.businessName = businessName;
        this.slug = slug;
        this.status = TenantStatus.ACTIVE;
        this.timezone = "Asia/Kolkata";
    }
    public static Tenant active(String businessName, String slug) { return new Tenant(businessName, slug); }
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getSlug() { return slug; }
    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone != null ? timezone : "Asia/Kolkata"; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
