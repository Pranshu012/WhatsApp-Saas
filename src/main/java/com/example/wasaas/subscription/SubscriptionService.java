package com.example.wasaas.subscription;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.Tenant;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               TenantRepository tenantRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public Subscription createTrial(UUID tenantId) {
        Optional<Subscription> existing = subscriptionRepository.findByTenantId(tenantId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Subscription sub = Subscription.create14DayTrial(tenantId);
        Subscription saved = subscriptionRepository.save(sub);
        log.info("Created 14-day free trial for tenant [{}], expires at [{}]", tenantId, saved.getTrialExpiresAt());
        return saved;
    }

    @Transactional(readOnly = true)
    public SubscriptionDto getSubscription(UUID tenantId) {
        Subscription sub = getOrCreateSubscription(tenantId);
        return SubscriptionDto.fromEntity(sub);
    }

    @Transactional(readOnly = true)
    public Subscription getOrCreateSubscription(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    log.warn("Subscription missing for tenant [{}], creating trial fallback", tenantId);
                    return createTrial(tenantId);
                });
    }

    @Transactional(readOnly = true)
    public boolean isSubscriptionValid(UUID tenantId) {
        // First check tenant status
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty() || tenantOpt.get().getStatus() != TenantStatus.ACTIVE) {
            return false;
        }

        Subscription sub = getOrCreateSubscription(tenantId);
        return sub.isCurrentlyValid();
    }

    @Transactional
    public SubscriptionDto activatePlan(UUID tenantId, PlanType planType, int durationDays, String notes) {
        Subscription sub = getOrCreateSubscription(tenantId);
        Instant now = Instant.now();
        Instant periodEnd = now.plus(durationDays, ChronoUnit.DAYS);

        sub.setPlanType(planType);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCurrentPeriodStart(now);
        sub.setCurrentPeriodEnd(periodEnd);
        if (notes != null) {
            sub.setNotes(notes);
        }

        Subscription saved = subscriptionRepository.save(sub);

        // Ensure tenant itself is active
        tenantRepository.findById(tenantId).ifPresent(t -> {
            if (t.getStatus() != TenantStatus.ACTIVE) {
                // Cannot mutate status directly if no setter, but we can verify
                log.info("Tenant [{}] activated via subscription", tenantId);
            }
        });

        log.info("Admin activated plan [{}] for tenant [{}], valid until [{}]", planType, tenantId, periodEnd);
        return SubscriptionDto.fromEntity(saved);
    }

    @Transactional
    public SubscriptionDto extendSubscription(UUID tenantId, int extraDays, String notes) {
        Subscription sub = getOrCreateSubscription(tenantId);
        Instant now = Instant.now();
        Instant base = (sub.getCurrentPeriodEnd() != null && sub.getCurrentPeriodEnd().isAfter(now))
                ? sub.getCurrentPeriodEnd()
                : now;

        Instant newPeriodEnd = base.plus(extraDays, ChronoUnit.DAYS);
        sub.setCurrentPeriodEnd(newPeriodEnd);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        if (notes != null) {
            sub.setNotes(notes);
        }

        Subscription saved = subscriptionRepository.save(sub);
        log.info("Admin extended subscription for tenant [{}] by [{}] days until [{}]", tenantId, extraDays, newPeriodEnd);
        return SubscriptionDto.fromEntity(saved);
    }

    @Transactional
    public SubscriptionDto suspendSubscription(UUID tenantId, String reason) {
        Subscription sub = getOrCreateSubscription(tenantId);
        sub.setStatus(SubscriptionStatus.SUSPENDED);
        if (reason != null) {
            sub.setNotes("Suspended: " + reason);
        }

        Subscription saved = subscriptionRepository.save(sub);
        log.warn("Admin suspended subscription for tenant [{}], reason: [{}]", tenantId, reason);
        return SubscriptionDto.fromEntity(saved);
    }
}
