package com.example.wasaas.template;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppTemplateRepository extends JpaRepository<WhatsAppTemplate, UUID> {

    Optional<WhatsAppTemplate> findByTenantIdAndNameAndLanguage(UUID tenantId, String name, String language);

    Optional<WhatsAppTemplate> findByTenantIdAndMetaTemplateId(UUID tenantId, String metaTemplateId);

    Optional<WhatsAppTemplate> findByMetaTemplateId(String metaTemplateId);

    Optional<WhatsAppTemplate> findByTenantIdAndId(UUID tenantId, UUID id);

    List<WhatsAppTemplate> findAllByTenantId(UUID tenantId);
}
