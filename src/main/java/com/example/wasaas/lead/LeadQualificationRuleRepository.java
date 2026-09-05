package com.example.wasaas.lead;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadQualificationRuleRepository extends JpaRepository<LeadQualificationRule, UUID> {

    List<LeadQualificationRule> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<LeadQualificationRule> findAllByTenantIdAndActiveTrue(UUID tenantId);

    Optional<LeadQualificationRule> findByTenantIdAndId(UUID tenantId, UUID id);
}
