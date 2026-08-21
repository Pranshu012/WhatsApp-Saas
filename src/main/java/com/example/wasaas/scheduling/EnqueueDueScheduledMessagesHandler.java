package com.example.wasaas.scheduling;

import com.example.wasaas.contact.Contact;
import com.example.wasaas.contact.ContactRepository;
import com.example.wasaas.job.Job;
import com.example.wasaas.job.JobHandler;
import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.template.WhatsAppTemplate;
import com.example.wasaas.template.WhatsAppTemplateRepository;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.client.TemplateComponent;
import com.example.wasaas.whatsapp.send.MessagingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class EnqueueDueScheduledMessagesHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(EnqueueDueScheduledMessagesHandler.class);
    public static final String JOB_TYPE = "ENQUEUE_DUE_SCHEDULED_MESSAGES";

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final ContactRepository contactRepository;
    private final WhatsAppTemplateRepository templateRepository;
    private final MessagingService messagingService;
    private final ObjectMapper objectMapper;

    public EnqueueDueScheduledMessagesHandler(ScheduledMessageRepository scheduledMessageRepository,
                                             ContactRepository contactRepository,
                                             WhatsAppTemplateRepository templateRepository,
                                             MessagingService messagingService,
                                             ObjectMapper objectMapper) {
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.contactRepository = contactRepository;
        this.templateRepository = templateRepository;
        this.messagingService = messagingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(Job job) throws Exception {
        Instant now = Instant.now();
        List<ScheduledMessage> dueMessages = scheduledMessageRepository.claimDue(now, 200);
        log.info("Claimed {} due scheduled messages for dispatch", dueMessages.size());

        for (ScheduledMessage sm : dueMessages) {
            TenantContext.set(sm.getTenantId());
            try {
                Contact contact = contactRepository.findById(sm.getContactId()).orElse(null);
                if (contact == null) {
                    log.error("Contact [{}] not found for scheduled message [{}]", sm.getContactId(), sm.getId());
                    sm.markFailed("Contact not found");
                    scheduledMessageRepository.save(sm);
                    continue;
                }

                WhatsAppTemplate template = templateRepository.findById(sm.getTemplateId()).orElse(null);
                if (template == null) {
                    log.error("Template [{}] not found for scheduled message [{}]", sm.getTemplateId(), sm.getId());
                    sm.markFailed("Template not found");
                    scheduledMessageRepository.save(sm);
                    continue;
                }

                List<TemplateComponent> components = List.of();
                if (sm.getVariables() != null && !sm.getVariables().isBlank()) {
                    try {
                        components = objectMapper.readValue(sm.getVariables(), new TypeReference<List<TemplateComponent>>() {});
                    } catch (Exception ignored) {}
                }

                BillingCategory category = template.getCategory() != null
                        ? BillingCategory.valueOf(template.getCategory().name())
                        : BillingCategory.MARKETING;

                // Deterministic idempotency key: sched:{id}
                String idempotencyKey = "sched:" + sm.getId();

                messagingService.sendTemplate(
                        sm.getWhatsappAccountId(),
                        contact.getPhoneE164(),
                        template.getName(),
                        template.getLanguage(),
                        components,
                        category,
                        idempotencyKey
                );

                sm.markEnqueued(null);
                scheduledMessageRepository.save(sm);
                log.info("Enqueued scheduled message [{}] with key [{}]", sm.getId(), idempotencyKey);

            } catch (Exception e) {
                log.error("Failed to enqueue scheduled message [{}]: {}", sm.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
