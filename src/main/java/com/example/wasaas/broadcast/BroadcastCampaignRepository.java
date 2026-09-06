package com.example.wasaas.broadcast;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BroadcastCampaignRepository extends JpaRepository<BroadcastCampaign, UUID> {

    List<BroadcastCampaign> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<BroadcastCampaign> findByTenantIdAndId(UUID tenantId, UUID id);

    List<BroadcastCampaign> findAllByStatusAndScheduledForLessThanEqual(BroadcastStatus status, Instant now);
}
