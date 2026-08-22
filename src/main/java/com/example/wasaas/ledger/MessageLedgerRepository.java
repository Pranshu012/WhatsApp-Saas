package com.example.wasaas.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageLedgerRepository extends JpaRepository<MessageLedger, UUID> {

    Optional<MessageLedger> findByWamid(String wamid);

    Optional<MessageLedger> findByIdempotencyKey(String idempotencyKey);

    List<MessageLedger> findAllByTenantIdAndRecipientPhoneHashOrderByCreatedAtAsc(UUID tenantId, String recipientPhoneHash);

    @Query("""
        SELECT l.billingCategory as category, COUNT(l) as total
        FROM MessageLedger l
        WHERE l.tenantId = :tenantId
          AND l.createdAt >= :start
          AND l.createdAt < :end
        GROUP BY l.billingCategory
    """)
    List<BillingCategoryCount> countByCategoryForDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
        SELECT l.status as status, COUNT(l) as total
        FROM MessageLedger l
        WHERE l.tenantId = :tenantId
          AND l.createdAt >= :start
          AND l.createdAt < :end
        GROUP BY l.status
    """)
    List<StatusOutcomeCount> countByStatusForDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("SELECT COUNT(l) FROM MessageLedger l WHERE l.tenantId = :tenantId AND l.createdAt >= :start AND l.createdAt < :end")
    long countByTenantIdAndDateRange(@Param("tenantId") UUID tenantId, @Param("start") Instant start, @Param("end") Instant end);

    @Query(value = "SELECT COUNT(*) FROM message_ledger WHERE created_at >= :start AND created_at < :end", nativeQuery = true)
    long countAllForDateRange(@Param("start") Instant start, @Param("end") Instant end);
}
