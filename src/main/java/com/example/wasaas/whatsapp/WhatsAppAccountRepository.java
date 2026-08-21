package com.example.wasaas.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsAppAccountRepository extends JpaRepository<WhatsAppAccount, UUID> {
    Optional<WhatsAppAccount> findByPhoneNumberId(String phoneNumberId);
    Optional<WhatsAppAccount> findByWabaId(String wabaId);
    List<WhatsAppAccount> findAllByStatus(WhatsAppAccountStatus status);
}
