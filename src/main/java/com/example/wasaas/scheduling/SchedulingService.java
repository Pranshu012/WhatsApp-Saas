package com.example.wasaas.scheduling;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.template.TemplateCategory;
import com.example.wasaas.template.TemplateService;
import com.example.wasaas.template.WhatsAppTemplate;
import com.example.wasaas.template.WhatsAppTemplateRepository;
import com.example.wasaas.tenant.context.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final WhatsAppTemplateRepository templateRepository;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    public SchedulingService(ScheduledMessageRepository scheduledMessageRepository,
                             WhatsAppTemplateRepository templateRepository,
                             TemplateService templateService,
                             ObjectMapper objectMapper) {
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ScheduledMessage scheduleMessage(ScheduleMessageCommand command) {
        UUID tenantId = TenantContext.require();

        if (command.templateId() == null) {
            throw new DomainException(HttpStatus.BAD_REQUEST,
                    "Scheduled messages must use an approved template, because the 24-hour service window will likely have expired.");
        }

        if (command.contactId() == null) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Contact ID cannot be null");
        }

        if (command.scheduledFor() == null || !command.scheduledFor().isAfter(Instant.now())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Scheduled time must be in the future");
        }

        WhatsAppTemplate template = templateRepository.findById(command.templateId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Template not found: " + command.templateId()));

        int paramCount = 0;
        if (command.components() != null) {
            for (var comp : command.components()) {
                if (comp.parameters() != null) {
                    paramCount += comp.parameters().size();
                }
            }
        }

        // Validate template is approved and parameters match
        templateService.assertSendable(tenantId, template.getName(), template.getLanguage(), paramCount);

        String componentsJson = null;
        if (command.components() != null && !command.components().isEmpty()) {
            try {
                componentsJson = objectMapper.writeValueAsString(command.components());
            } catch (Exception ignored) {}
        }

        ScheduledMessage message = new ScheduledMessage(
                tenantId,
                command.contactId(),
                command.templateId(),
                command.whatsappAccountId(),
                componentsJson,
                command.scheduledFor(),
                command.timezone()
        );

        ScheduledMessage saved = scheduledMessageRepository.save(message);
        log.info("Scheduled message [{}] for contact [{}] at [{}] UTC (tenant: {})",
                saved.getId(), saved.getContactId(), saved.getScheduledFor(), tenantId);

        return saved;
    }

    @Transactional
    public void cancelScheduledMessage(UUID scheduledMessageId) {
        UUID tenantId = TenantContext.require();

        ScheduledMessage message = scheduledMessageRepository.findByTenantIdAndId(tenantId, scheduledMessageId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Scheduled message not found: " + scheduledMessageId));

        message.cancel();
        scheduledMessageRepository.save(message);
        log.info("Cancelled scheduled message [{}] for tenant [{}]", scheduledMessageId, tenantId);
    }
}
