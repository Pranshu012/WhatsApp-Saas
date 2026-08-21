package com.example.wasaas.automation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UnmatchedMessageRepository extends JpaRepository<UnmatchedMessage, UUID> {

    List<UnmatchedMessage> findAllByTenantIdOrderByReceivedAtDesc(UUID tenantId);
}
