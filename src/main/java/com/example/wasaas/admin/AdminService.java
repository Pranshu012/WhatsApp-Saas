package com.example.wasaas.admin;

import com.example.wasaas.automation.AutomationRuleRepository;
import com.example.wasaas.automation.faq.FaqRepository;
import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.ledger.MessageLedgerRepository;
import com.example.wasaas.subscription.PlanType;
import com.example.wasaas.subscription.Subscription;
import com.example.wasaas.subscription.SubscriptionRepository;
import com.example.wasaas.subscription.SubscriptionService;
import com.example.wasaas.subscription.SubscriptionStatus;
import com.example.wasaas.tenant.Tenant;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantStatus;
import com.example.wasaas.tenant.TenantUser;
import com.example.wasaas.tenant.TenantUserRepository;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.user.User;
import com.example.wasaas.user.UserRepository;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountRepository;
import com.example.wasaas.whatsapp.WhatsAppAccountStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantUserRepository tenantUserRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final WhatsAppAccountRepository whatsAppAccountRepository;
    private final FaqRepository faqRepository;
    private final AutomationRuleRepository automationRuleRepository;
    private final MessageLedgerRepository messageLedgerRepository;

    public AdminService(TenantRepository tenantRepository,
                        UserRepository userRepository,
                        TenantUserRepository tenantUserRepository,
                        SubscriptionRepository subscriptionRepository,
                        SubscriptionService subscriptionService,
                        WhatsAppAccountRepository whatsAppAccountRepository,
                        FaqRepository faqRepository,
                        AutomationRuleRepository automationRuleRepository,
                        MessageLedgerRepository messageLedgerRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.whatsAppAccountRepository = whatsAppAccountRepository;
        this.faqRepository = faqRepository;
        this.automationRuleRepository = automationRuleRepository;
        this.messageLedgerRepository = messageLedgerRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTenantDto> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        List<AdminTenantDto> dtos = new ArrayList<>();

        for (Tenant tenant : tenants) {
            dtos.add(buildTenantDto(tenant));
        }

        return dtos;
    }

    @Transactional(readOnly = true)
    public AdminTenantDto getTenantDetails(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Tenant not found: " + tenantId));
        return buildTenantDto(tenant);
    }

    @Transactional
    public AdminTenantDto activateTenant(UUID tenantId, ActivateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Tenant not found: " + tenantId));

        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);

        PlanType plan = PlanType.valueOf(request.planType().toUpperCase());
        subscriptionService.activatePlan(tenantId, plan, request.durationDays(), request.notes());

        return buildTenantDto(tenant);
    }

    @Transactional
    public AdminTenantDto extendTenantSubscription(UUID tenantId, ExtendSubscriptionRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Tenant not found: " + tenantId));

        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);

        subscriptionService.extendSubscription(tenantId, request.extraDays(), request.notes());
        return buildTenantDto(tenant);
    }

    @Transactional
    public AdminTenantDto suspendTenant(UUID tenantId, SuspendTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Tenant not found: " + tenantId));

        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);

        subscriptionService.suspendSubscription(tenantId, request != null ? request.reason() : "Suspended by admin");
        return buildTenantDto(tenant);
    }

    @Transactional(readOnly = true)
    public AdminPlatformStatsDto getPlatformStats() {
        List<Tenant> allTenants = tenantRepository.findAll();
        long totalTenants = allTenants.size();
        long activeTenants = 0;
        long trialingTenants = 0;
        long suspendedTenants = 0;
        long activeWhatsApp = 0;
        Map<String, Long> planDistribution = new HashMap<>();

        for (Tenant t : allTenants) {
            Subscription sub = subscriptionService.getOrCreateSubscription(t.getId());
            if (t.getStatus() == TenantStatus.SUSPENDED || sub.getStatus() == SubscriptionStatus.SUSPENDED) {
                suspendedTenants++;
            } else if (sub.getStatus() == SubscriptionStatus.TRIALING) {
                trialingTenants++;
            } else if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                activeTenants++;
            }

            String planName = sub.getPlanType().name();
            planDistribution.put(planName, planDistribution.getOrDefault(planName, 0L) + 1);

            // Check if tenant has connected whatsapp
            try {
                TenantContext.set(t.getId());
                var account = whatsAppAccountRepository.findByTenantId(t.getId());
                if (account.isPresent() && account.get().getStatus() == WhatsAppAccountStatus.CONNECTED) {
                    activeWhatsApp++;
                }
            } finally {
                TenantContext.clear();
            }
        }

        long totalUsers = userRepository.count();
        long estMonthlyRev = activeTenants * 499;

        // Message ledger total this month across all tenants
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        YearMonth currentYearMonth = YearMonth.now(zone);
        ZonedDateTime startOfMonth = currentYearMonth.atDay(1).atStartOfDay(zone);
        ZonedDateTime endOfMonth = currentYearMonth.plusMonths(1).atDay(1).atStartOfDay(zone);
        long totalMessagesThisMonth = messageLedgerRepository.countAllForDateRange(startOfMonth.toInstant(), endOfMonth.toInstant());

        return new AdminPlatformStatsDto(
                totalTenants,
                activeTenants,
                trialingTenants,
                suspendedTenants,
                totalUsers,
                totalMessagesThisMonth,
                activeWhatsApp,
                estMonthlyRev,
                planDistribution
        );
    }

    private AdminTenantDto buildTenantDto(Tenant tenant) {
        UUID tId = tenant.getId();
        UUID ownerId = null;
        String ownerName = null;
        String ownerEmail = null;

        // Resolve Owner User
        List<TenantUser> memberships = tenantUserRepository.findByIdTenantId(tId);
        if (!memberships.isEmpty()) {
            TenantUser ownerMembership = memberships.get(0);
            Optional<User> uOpt = userRepository.findById(ownerMembership.getId().getUserId());
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                ownerId = u.getId();
                ownerName = u.getFullName();
                ownerEmail = u.getEmail();
            }
        }

        // WhatsApp details
        boolean waConnected = false;
        String displayPhone = null;
        String qualityRating = null;
        String limitTier = null;

        long msgCount = 0;
        long faqCount = 0;
        long ruleCount = 0;

        try {
            TenantContext.set(tId);
            Optional<WhatsAppAccount> waOpt = whatsAppAccountRepository.findByTenantId(tId);
            if (waOpt.isPresent() && waOpt.get().getStatus() == WhatsAppAccountStatus.CONNECTED) {
                WhatsAppAccount wa = waOpt.get();
                waConnected = true;
                displayPhone = wa.getDisplayPhoneNumber();
                qualityRating = wa.getQualityRating();
                limitTier = wa.getMessagingLimitTier();
            }

            faqCount = faqRepository.count();
            ruleCount = automationRuleRepository.count();

            ZoneId zone = ZoneId.of("Asia/Kolkata");
            YearMonth currentYearMonth = YearMonth.now(zone);
            ZonedDateTime startOfMonth = currentYearMonth.atDay(1).atStartOfDay(zone);
            ZonedDateTime endOfMonth = currentYearMonth.plusMonths(1).atDay(1).atStartOfDay(zone);
            msgCount = messageLedgerRepository.countByTenantIdAndDateRange(tId, startOfMonth.toInstant(), endOfMonth.toInstant());
        } catch (Exception e) {
            // Ignore stats errors per tenant
        } finally {
            TenantContext.clear();
        }

        // Subscription details
        Subscription sub = subscriptionService.getOrCreateSubscription(tId);

        return new AdminTenantDto(
                tenant.getId(),
                tenant.getBusinessName(),
                tenant.getSlug(),
                tenant.getStatus().name(),
                tenant.getTimezone(),
                tenant.getGstin(),
                tenant.getLegalName(),
                tenant.getBillingAddress(),
                tenant.getCreatedAt(),
                ownerId,
                ownerName,
                ownerEmail,
                waConnected,
                displayPhone,
                qualityRating,
                limitTier,
                sub.getId(),
                sub.getPlanType().name(),
                sub.getStatus().name(),
                sub.getTrialStartDate(),
                sub.getTrialExpiresAt(),
                sub.getCurrentPeriodStart(),
                sub.getCurrentPeriodEnd(),
                sub.getDaysRemaining(),
                sub.isCurrentlyValid() && tenant.getStatus() == TenantStatus.ACTIVE,
                sub.getMonthlyPricePaise(),
                sub.getNotes(),
                msgCount,
                faqCount,
                ruleCount
        );
    }
}
