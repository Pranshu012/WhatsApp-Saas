package com.example.wasaas.tenant;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserRepository extends JpaRepository<TenantUser, TenantUserId> {
    List<TenantUser> findByIdUserId(UUID userId);
    List<TenantUser> findByIdTenantId(UUID tenantId);
}
