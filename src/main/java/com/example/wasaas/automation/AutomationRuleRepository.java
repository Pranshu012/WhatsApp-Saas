package com.example.wasaas.automation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID> {

    List<AutomationRule> findAllByTenantIdAndEnabledTrueOrderByPriorityAsc(UUID tenantId);

    List<AutomationRule> findAllByTenantIdOrderByPriorityAsc(UUID tenantId);
}
