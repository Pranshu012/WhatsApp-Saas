package com.example.wasaas.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByTenantId(UUID tenantId);

    @Query(value = "SELECT * FROM subscriptions WHERE tenant_id = :tenantId", nativeQuery = true)
    Optional<Subscription> findByTenantIdNative(@Param("tenantId") UUID tenantId);

    boolean existsByTenantId(UUID tenantId);
}
