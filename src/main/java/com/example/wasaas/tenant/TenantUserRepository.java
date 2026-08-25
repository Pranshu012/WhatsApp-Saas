package com.example.wasaas.tenant;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantUserRepository extends JpaRepository<TenantUser, TenantUserId> {
    @Query(value = "SELECT * FROM get_user_tenant_memberships(:userId)", nativeQuery = true)
    List<TenantUser> findByIdUserId(@Param("userId") UUID userId);

    List<TenantUser> findByIdTenantId(UUID tenantId);
}
