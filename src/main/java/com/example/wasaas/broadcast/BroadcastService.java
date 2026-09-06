package com.example.wasaas.broadcast;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.contact.Contact;
import com.example.wasaas.contact.ContactRepository;
import com.example.wasaas.job.JobService;
import com.example.wasaas.lead.Lead;
import com.example.wasaas.lead.LeadRepository;
import com.example.wasaas.lead.LeadStage;
import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountRepository;
import com.example.wasaas.whatsapp.client.TemplateComponent;
import com.example.wasaas.whatsapp.client.TemplateParameter;
import com.example.wasaas.whatsapp.send.MessagingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service orchestrating bulk WhatsApp marketing campaigns.
 * <p>
 * Core capabilities:
 * <ul>
 *   <li><b>Audience Segmentation:</b> Resolves target recipients by CRM stage, contact books, or uploaded lists.</li>
 *   <li><b>Mandatory Opt-Out Filtering:</b> Enforces international compliance by excluding any contacts marked as {@code OPTED_OUT}.</li>
 *   <li><b>Chunked Background Dispatch:</b> Enqueues campaign dispatch jobs into the transactional jobs table for reliable parallel delivery.</li>
 *   <li><b>Real-Time Analytics:</b> Tracks sent, delivered, read, and failed counts per campaign.</li>
 * </ul>
 */
@Service
public class BroadcastService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);
    private static final Pattern DIGITS_ONLY = Pattern.compile("\\D");

    private final BroadcastCampaignRepository campaignRepository;
    private final BroadcastRecipientRepository recipientRepository;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final WhatsAppAccountRepository whatsAppAccountRepository;
    private final MessagingService messagingService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;

    public BroadcastService(BroadcastCampaignRepository campaignRepository,
                            BroadcastRecipientRepository recipientRepository,
                            ContactRepository contactRepository,
                            LeadRepository leadRepository,
                            WhatsAppAccountRepository whatsAppAccountRepository,
                            MessagingService messagingService,
                            JobService jobService,
                            ObjectMapper objectMapper) {
        this.campaignRepository = campaignRepository;
        this.recipientRepository = recipientRepository;
        this.contactRepository = contactRepository;
        this.leadRepository = leadRepository;
        this.whatsAppAccountRepository = whatsAppAccountRepository;
        this.messagingService = messagingService;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<BroadcastCampaignDto> listCampaigns() {
        UUID tenantId = TenantContext.require();
        List<BroadcastCampaign> campaigns = campaignRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        return campaigns.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BroadcastCampaignDetailDto getCampaign(UUID id) {
        UUID tenantId = TenantContext.require();
        BroadcastCampaign campaign = campaignRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Campaign not found: " + id));

        List<BroadcastRecipient> recipients = recipientRepository.findAllByBroadcastId(campaign.getId());
        List<BroadcastRecipientDto> recipientDtos = recipients.stream().map(this::toRecipientDto).toList();

        return new BroadcastCampaignDetailDto(toDto(campaign), recipientDtos);
    }

    @Transactional
    public BroadcastCampaignDto createCampaign(CreateBroadcastRequest request) {
        UUID tenantId = TenantContext.require();

        if (request.name() == null || request.name().isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Campaign name is required");
        }

        WhatsAppAccount account = whatsAppAccountRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "No WhatsApp account connected for tenant. Please connect in Settings first."));

        String paramsJson = null;
        if (request.templateParams() != null && !request.templateParams().isEmpty()) {
            try {
                paramsJson = objectMapper.writeValueAsString(request.templateParams());
            } catch (Exception ignored) {}
        }

        BroadcastCampaign campaign = new BroadcastCampaign(
                tenantId,
                account.getId(),
                request.name().trim(),
                request.targetType() != null ? request.targetType() : TargetType.ALL_CONTACTS,
                request.targetStage(),
                request.templateName(),
                request.templateLanguage() != null ? request.templateLanguage() : "en_US",
                request.templateId(),
                paramsJson,
                request.messagePreview(),
                request.scheduledFor()
        );

        BroadcastCampaign savedCampaign = campaignRepository.save(campaign);

        // Resolve Target Audience
        List<ResolvedRecipient> resolvedRecipients = resolveAudience(tenantId, request);

        // Deduplicate by phone
        Map<String, ResolvedRecipient> uniqueRecipients = new LinkedHashMap<>();
        for (ResolvedRecipient r : resolvedRecipients) {
            String norm = normalizePhone(r.phone());
            if (norm != null && !norm.isBlank()) {
                uniqueRecipients.putIfAbsent(norm, new ResolvedRecipient(norm, r.name(), r.contactId()));
            }
        }

        List<BroadcastRecipient> recipientsToSave = new ArrayList<>();
        for (ResolvedRecipient r : uniqueRecipients.values()) {
            recipientsToSave.add(new BroadcastRecipient(
                    savedCampaign.getId(),
                    tenantId,
                    r.contactId(),
                    r.phone(),
                    r.name(),
                    null
            ));
        }

        recipientRepository.saveAll(recipientsToSave);

        savedCampaign.setTotalRecipients(recipientsToSave.size());
        savedCampaign = campaignRepository.save(savedCampaign);

        // If not scheduled for later, trigger dispatch immediately
        if (request.scheduledFor() == null || request.scheduledFor().isBefore(Instant.now().plusSeconds(60))) {
            savedCampaign.setStatus(BroadcastStatus.RUNNING);
            savedCampaign.setStartedAt(Instant.now());
            savedCampaign = campaignRepository.save(savedCampaign);

            enqueueDispatchJob(tenantId, savedCampaign.getId());
        }

        return toDto(savedCampaign);
    }

    @Transactional
    public void cancelCampaign(UUID id) {
        UUID tenantId = TenantContext.require();
        BroadcastCampaign campaign = campaignRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Campaign not found: " + id));

        if (campaign.getStatus() == BroadcastStatus.COMPLETED) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Cannot cancel an already completed campaign.");
        }

        campaign.setStatus(BroadcastStatus.CANCELLED);
        campaignRepository.save(campaign);
    }

    /**
     * Called by DispatchBroadcastJobHandler in the background.
     */
    @Transactional
    public void dispatchCampaignInternal(UUID campaignId) {
        BroadcastCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null || campaign.getStatus() == BroadcastStatus.CANCELLED) {
            return;
        }

        TenantContext.set(campaign.getTenantId());
        try {
            campaign.setStatus(BroadcastStatus.RUNNING);
            if (campaign.getStartedAt() == null) {
                campaign.setStartedAt(Instant.now());
            }
            campaignRepository.save(campaign);

            List<BroadcastRecipient> pending = recipientRepository.findAllByBroadcastIdAndStatus(
                    campaign.getId(), RecipientStatus.PENDING);

            Map<String, String> paramTemplate = parseParams(campaign.getTemplateParams());

            int sentCount = campaign.getSentCount();
            int failedCount = campaign.getFailedCount();

            for (BroadcastRecipient recipient : pending) {
                try {
                    // Check if campaign got cancelled mid-way
                    BroadcastCampaign check = campaignRepository.findById(campaignId).orElse(null);
                    if (check != null && check.getStatus() == BroadcastStatus.CANCELLED) {
                        break;
                    }

                    String idempotencyKey = "bcast:" + campaign.getId() + ":" + recipient.getId();
                    boolean hasMetaTemplate = campaign.getTemplateId() != null
                            && campaign.getTemplateName() != null
                            && !campaign.getTemplateName().isBlank()
                            && !campaign.getTemplateName().startsWith("custom_")
                            && !campaign.getTemplateName().startsWith("ai_")
                            && !campaign.getTemplateName().equalsIgnoreCase("DIRECT_TEXT");

                    if (hasMetaTemplate) {
                        List<TemplateComponent> components = buildComponents(paramTemplate, recipient.getContactName());
                        messagingService.sendTemplate(
                                campaign.getWhatsappAccountId(),
                                recipient.getPhoneE164(),
                                campaign.getTemplateName(),
                                campaign.getTemplateLanguage(),
                                components,
                                BillingCategory.MARKETING,
                                idempotencyKey
                        );
                    } else {
                        // Direct / AI / Custom broadcast text message
                        String rawText = campaign.getMessagePreview();
                        if (rawText == null || rawText.isBlank()) {
                            rawText = "Hello {{name}}! We have a special update for you.";
                        }
                        String nameReplacement = (recipient.getContactName() != null && !recipient.getContactName().isBlank())
                                ? recipient.getContactName()
                                : "Customer";
                        String finalText = rawText.replace("{{name}}", nameReplacement);
                        messagingService.sendText(
                                campaign.getWhatsappAccountId(),
                                recipient.getPhoneE164(),
                                finalText,
                                idempotencyKey
                        );
                    }

                    recipient.setStatus(RecipientStatus.SENT);
                    recipient.setSentAt(Instant.now());
                    sentCount++;
                } catch (Exception ex) {
                    log.warn("Failed to dispatch broadcast to {}: {}", recipient.getPhoneE164(), ex.getMessage());
                    recipient.setStatus(RecipientStatus.FAILED);
                    recipient.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "Dispatch error");
                    failedCount++;
                }
                recipientRepository.save(recipient);

                // Polite pacing to prevent hitting burst rate-limits
                try {
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {}
            }

            campaign.setSentCount(sentCount);
            campaign.setFailedCount(failedCount);
            campaign.setStatus(failedCount > 0 && sentCount == 0 ? BroadcastStatus.FAILED : BroadcastStatus.COMPLETED);
            campaign.setCompletedAt(Instant.now());
            campaignRepository.save(campaign);

        } finally {
            TenantContext.clear();
        }
    }

    private void enqueueDispatchJob(UUID tenantId, UUID campaignId) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of("campaignId", campaignId.toString()));
            jobService.enqueue(tenantId, "DISPATCH_BROADCAST_CAMPAIGN", payload, "dispatch:" + campaignId);
        } catch (Exception e) {
            log.error("Failed to enqueue dispatch job for campaign {}", campaignId, e);
        }
    }

    private List<ResolvedRecipient> resolveAudience(UUID tenantId, CreateBroadcastRequest request) {
        List<ResolvedRecipient> list = new ArrayList<>();
        TargetType targetType = request.targetType() != null ? request.targetType() : TargetType.ALL_CONTACTS;

        switch (targetType) {
            case ALL_CONTACTS -> {
                List<Contact> contacts = contactRepository.findAllByTenantId(tenantId);
                for (Contact c : contacts) {
                    if (c.getOptInStatus() == null || !"OPTED_OUT".equalsIgnoreCase(c.getOptInStatus())) {
                        list.add(new ResolvedRecipient(c.getPhoneE164(), c.getDisplayName(), c.getId()));
                    } else {
                        log.debug("Excluding opted-out contact [{}] from broadcast", c.getPhoneE164());
                    }
                }
            }
            case LEADS_BY_STAGE -> {
                List<Lead> leads = leadRepository.findAllByTenantIdOrderByLastInteractionAtDesc(tenantId);
                String targetStage = request.targetStage();
                for (Lead l : leads) {
                    if (targetStage == null || targetStage.equalsIgnoreCase("ALL") || l.getStage().name().equalsIgnoreCase(targetStage)) {
                        Contact c = contactRepository.findById(l.getContactId()).orElse(null);
                        if (c != null && (c.getOptInStatus() == null || !"OPTED_OUT".equalsIgnoreCase(c.getOptInStatus()))) {
                            list.add(new ResolvedRecipient(c.getPhoneE164(), c.getDisplayName(), c.getId()));
                        }
                    }
                }
            }
            case CSV_UPLOAD, PASTE_NUMBERS -> {
                if (request.customRecipients() != null) {
                    for (CustomRecipientInput input : request.customRecipients()) {
                        if (input.phoneE164() != null && !input.phoneE164().isBlank()) {
                            String norm = normalizePhone(input.phoneE164());
                            if (norm != null) {
                                Optional<Contact> existing = contactRepository.findByTenantIdAndPhoneE164(tenantId, norm);
                                if (existing.isPresent() && "OPTED_OUT".equalsIgnoreCase(existing.get().getOptInStatus())) {
                                    log.info("Excluding opted-out phone [{}] from broadcast under tenant [{}]", norm, tenantId);
                                    continue;
                                }
                            }
                            list.add(new ResolvedRecipient(input.phoneE164().trim(), input.name(), null));
                        }
                    }
                }
            }
        }
        return list;
    }

    private List<TemplateComponent> buildComponents(Map<String, String> paramTemplate, String contactName) {
        if (paramTemplate == null || paramTemplate.isEmpty()) {
            return List.of();
        }

        List<TemplateParameter> bodyParams = new ArrayList<>();
        // Sort by param index 1, 2, 3...
        List<String> sortedKeys = new ArrayList<>(paramTemplate.keySet());
        sortedKeys.sort(Comparator.comparingInt(k -> {
            try { return Integer.parseInt(k); } catch (Exception e) { return 999; }
        }));

        for (String key : sortedKeys) {
            String val = paramTemplate.get(key);
            if (val != null) {
                // Replace {{name}} with actual contact name or "Customer"
                String replaced = val.replace("{{name}}", (contactName != null && !contactName.isBlank()) ? contactName : "Customer");
                bodyParams.add(TemplateParameter.text(replaced));
            }
        }

        if (bodyParams.isEmpty()) {
            return List.of();
        }
        return List.of(TemplateComponent.body(bodyParams));
    }

    private Map<String, String> parseParams(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = DIGITS_ONLY.matcher(raw).replaceAll("");
        if (digits.isBlank()) return null;

        // If starts with + (e.g. +91...)
        if (raw.trim().startsWith("+")) {
            return "+" + digits;
        }
        // Indian 10-digit number
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        // Indian with country code 919876543210
        if (digits.length() == 12 && digits.startsWith("91")) {
            return "+" + digits;
        }
        // Indian with leading 0: 09876543210
        if (digits.length() == 11 && digits.startsWith("0")) {
            return "+91" + digits.substring(1);
        }
        return "+" + digits;
    }

    private BroadcastCampaignDto toDto(BroadcastCampaign c) {
        int deliveryRate = c.getTotalRecipients() > 0
                ? Math.round(((float) c.getSentCount() / c.getTotalRecipients()) * 100)
                : 0;

        return new BroadcastCampaignDto(
                c.getId(),
                c.getName(),
                c.getTargetType(),
                c.getTargetStage(),
                c.getTemplateName(),
                c.getTemplateLanguage(),
                c.getMessagePreview(),
                c.getStatus(),
                c.getTotalRecipients(),
                c.getSentCount(),
                c.getFailedCount(),
                deliveryRate,
                c.getScheduledFor(),
                c.getStartedAt(),
                c.getCompletedAt(),
                c.getCreatedAt()
        );
    }

    private BroadcastRecipientDto toRecipientDto(BroadcastRecipient r) {
        return new BroadcastRecipientDto(
                r.getId(),
                r.getPhoneE164(),
                r.getContactName(),
                r.getStatus(),
                r.getErrorMessage(),
                r.getSentAt(),
                r.getCreatedAt()
        );
    }

    private record ResolvedRecipient(String phone, String name, UUID contactId) {}

    public record CreateBroadcastRequest(
            String name,
            TargetType targetType,
            String targetStage,
            String templateName,
            String templateLanguage,
            UUID templateId,
            Map<String, String> templateParams,
            String messagePreview,
            Instant scheduledFor,
            List<CustomRecipientInput> customRecipients
    ) {}

    public record CustomRecipientInput(
            String phoneE164,
            String name
    ) {}

    public record BroadcastCampaignDto(
            UUID id,
            String name,
            TargetType targetType,
            String targetStage,
            String templateName,
            String templateLanguage,
            String messagePreview,
            BroadcastStatus status,
            int totalRecipients,
            int sentCount,
            int failedCount,
            int deliveryRatePercent,
            Instant scheduledFor,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt
    ) {}

    public record BroadcastRecipientDto(
            UUID id,
            String phoneE164,
            String contactName,
            RecipientStatus status,
            String errorMessage,
            Instant sentAt,
            Instant createdAt
    ) {}

    public record BroadcastCampaignDetailDto(
            BroadcastCampaignDto campaign,
            List<BroadcastRecipientDto> recipients
    ) {}
}
