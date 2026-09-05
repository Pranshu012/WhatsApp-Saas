package com.example.wasaas.contact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findTop8ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    List<ConversationMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
