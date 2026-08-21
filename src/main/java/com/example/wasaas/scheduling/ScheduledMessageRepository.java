package com.example.wasaas.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, UUID> {

    List<ScheduledMessage> findAllByTenantId(UUID tenantId);

    Optional<ScheduledMessage> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query(value = """
            SELECT sm.* FROM scheduled_messages sm
            WHERE sm.status = 'SCHEDULED'
              AND sm.scheduled_for <= :now
            ORDER BY sm.scheduled_for ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<ScheduledMessage> claimDue(@Param("now") Instant now, @Param("limit") int limit);
}
