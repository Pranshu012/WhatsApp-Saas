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

    protected Tenant() { }
    private Tenant(String businessName, String slug) { this.id = UUID.randomUUID(); this.businessName = businessName; this.slug = slug; this.status = TenantStatus.ACTIVE; }
    public static Tenant active(String businessName, String slug) { return new Tenant(businessName, slug); }
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getBusinessName() { return businessName; }
    public String getSlug() { return slug; }
    public TenantStatus getStatus() { return status; }
}
