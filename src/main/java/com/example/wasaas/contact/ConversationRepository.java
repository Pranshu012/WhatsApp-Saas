package com.example.wasaas.contact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByTenantIdAndContactIdAndWhatsappAccountId(
            UUID tenantId, UUID contactId, UUID whatsappAccountId);
}
