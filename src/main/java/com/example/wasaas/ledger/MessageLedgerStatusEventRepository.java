package com.example.wasaas.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageLedgerStatusEventRepository extends JpaRepository<MessageLedgerStatusEvent, UUID> {

    List<MessageLedgerStatusEvent> findByLedgerIdOrderByOccurredAtAsc(UUID ledgerId);
}
