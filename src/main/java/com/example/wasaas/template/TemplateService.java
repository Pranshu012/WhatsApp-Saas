package com.example.wasaas.template;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.job.PermanentJobException;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.meta.CreateMetaTemplateRequest;
import com.example.wasaas.whatsapp.meta.MetaGraphClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private final WhatsAppTemplateRepository templateRepository;
    private final WhatsAppAccountService accountService;
    private final MetaGraphClient metaGraphClient;
    private final ObjectMapper objectMapper;

    public TemplateService(WhatsAppTemplateRepository templateRepository,
                           WhatsAppAccountService accountService,
                           MetaGraphClient metaGraphClient,
                           ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.accountService = accountService;
        this.metaGraphClient = metaGraphClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WhatsAppTemplate submitForApproval(CreateTemplateCommand command) {
        UUID tenantId = TenantContext.require();

        WhatsAppAccount account = accountService.getAccount(command.whatsappAccountId());
        String accessToken = accountService.getDecryptedToken(account.getId());

        CreateMetaTemplateRequest metaRequest = new CreateMetaTemplateRequest(
                command.name(),
                command.category().name(),
                true, // allow_category_change
                command.language(),
                command.components()
        );

        String metaTemplateId = metaGraphClient.createTemplate(account.getWabaId(), accessToken, metaRequest);

        String componentsJson = null;
        if (command.components() != null && !command.components().isEmpty()) {
            try {
                componentsJson = objectMapper.writeValueAsString(command.components());
            } catch (Exception ignored) {}
        }

        WhatsAppTemplate template = new WhatsAppTemplate(
                tenantId,
                command.whatsappAccountId(),
                metaTemplateId,
                command.name(),
                command.language(),
                command.category(),
                null, // Assigned category from Meta arrives on sync
                TemplateStatus.PENDING,
                null,
                command.bodyText(),
                null,
                componentsJson
        );

        WhatsAppTemplate saved = templateRepository.save(template);
        log.info("Submitted template [{}] for approval with Meta ID [{}] for tenant [{}]",
                command.name(), metaTemplateId, tenantId);

        return saved;
    }

    public WhatsAppTemplate assertSendable(UUID tenantId, String templateName, String language, int paramCount) {
        if (templateName == null || templateName.isBlank()) {
            throw new PermanentJobException("Template name cannot be empty");
        }

        String lang = (language != null && !language.isBlank()) ? language : "en_US";

        WhatsAppTemplate template = templateRepository.findByTenantIdAndNameAndLanguage(tenantId, templateName, lang)
                .orElseThrow(() -> new PermanentJobException("Template '" + templateName + "' with language '" + lang + "' not found for tenant " + tenantId));

        if (template.getStatus() != TemplateStatus.APPROVED) {
            throw new PermanentJobException("Template '" + templateName + "' is " + template.getStatus() + ", not APPROVED");
        }

        if (paramCount != template.getVariableCount()) {
            throw new PermanentJobException("Template '" + templateName + "' variable count mismatch: expected "
                    + template.getVariableCount() + ", got " + paramCount);
        }

        return template;
    }
}
