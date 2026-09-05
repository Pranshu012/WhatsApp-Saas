package com.example.wasaas.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TenantAiConfigRepository extends JpaRepository<TenantAiConfig, UUID> {
}
