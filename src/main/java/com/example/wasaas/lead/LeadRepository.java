package com.example.wasaas.lead;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    List<Lead> findAllByTenantIdOrderByLastInteractionAtDesc(UUID tenantId);

    Optional<Lead> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Lead> findByTenantIdAndContactId(UUID tenantId, UUID contactId);

    List<Lead> findAllByTenantIdAndStage(UUID tenantId, LeadStage stage);

    long countByTenantIdAndStage(UUID tenantId, LeadStage stage);
}
