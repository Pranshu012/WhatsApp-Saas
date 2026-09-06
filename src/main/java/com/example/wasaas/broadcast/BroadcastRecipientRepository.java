package com.example.wasaas.broadcast;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BroadcastRecipientRepository extends JpaRepository<BroadcastRecipient, UUID> {

    List<BroadcastRecipient> findAllByBroadcastId(UUID broadcastId);

    List<BroadcastRecipient> findAllByBroadcastIdAndStatus(UUID broadcastId, RecipientStatus status);

    long countByBroadcastIdAndStatus(UUID broadcastId, RecipientStatus status);
}
