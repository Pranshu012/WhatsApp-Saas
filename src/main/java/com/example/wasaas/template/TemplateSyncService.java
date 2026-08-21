package com.example.wasaas.template;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.meta.MetaGraphClient;
import com.example.wasaas.whatsapp.meta.MetaTemplateItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TemplateSyncService {

    private static final Logger log = LoggerFactory.getLogger(TemplateSyncService.class);

    private final WhatsAppTemplateRepository templateRepository;
    private final WhatsAppAccountService accountService;
    private final MetaGraphClient metaGraphClient;
    private final ObjectMapper objectMapper;

    public TemplateSyncService(WhatsAppTemplateRepository templateRepository,
                               WhatsAppAccountService accountService,
                               MetaGraphClient metaGraphClient,
                               ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.accountService = accountService;
        this.metaGraphClient = metaGraphClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int syncTemplates(UUID tenantId, UUID whatsappAccountId) {
        TenantContext.set(tenantId);
        try {
            WhatsAppAccount account = accountService.getAccount(whatsappAccountId);
            String accessToken = accountService.getDecryptedToken(account.getId());
            List<MetaTemplateItem> remoteTemplates = metaGraphClient.listTemplates(account.getWabaId(), accessToken);

            Instant syncTime = Instant.now();
            int count = 0;

            for (MetaTemplateItem item : remoteTemplates) {
                TemplateCategory assignedCategory = parseCategory(item.category());
                TemplateStatus status = parseStatus(item.status());

                String bodyText = "";
                String headerType = null;
                String componentsJson = null;

                if (item.components() != null && !item.components().isEmpty()) {
                    try {
                        componentsJson = objectMapper.writeValueAsString(item.components());
                    } catch (Exception ignored) {}

                    for (JsonNode comp : item.components()) {
                        if (comp.has("type")) {
                            String type = comp.get("type").asText();
                            if ("BODY".equalsIgnoreCase(type) && comp.has("text")) {
                                bodyText = comp.get("text").asText();
                            } else if ("HEADER".equalsIgnoreCase(type) && comp.has("format")) {
                                headerType = comp.get("format").asText();
                            }
                        }
                    }
                }

                Optional<WhatsAppTemplate> existingOpt = templateRepository.findByTenantIdAndNameAndLanguage(
                        tenantId, item.name(), item.language());

                if (existingOpt.isPresent()) {
                    WhatsAppTemplate template = existingOpt.get();
                    template.updateFromMeta(
                            item.id(),
                            assignedCategory,
                            status,
                            item.rejectionReason(),
                            bodyText,
                            headerType,
                            componentsJson,
                            syncTime
                    );
                    templateRepository.save(template);
                    log.info("Updated template [{}] (category={}, conflict={}) for tenant [{}]",
                            item.name(), assignedCategory, template.isCategoryConflict(), tenantId);
                } else {
                    WhatsAppTemplate newTemplate = new WhatsAppTemplate(
                            tenantId,
                            whatsappAccountId,
                            item.id(),
                            item.name(),
                            item.language(),
                            null, // No previous requested category
                            assignedCategory,
                            status,
                            item.rejectionReason(),
                            bodyText,
                            headerType,
                            componentsJson
                    );
                    newTemplate.updateFromMeta(
                            item.id(),
                            assignedCategory,
                            status,
                            item.rejectionReason(),
                            bodyText,
                            headerType,
                            componentsJson,
                            syncTime
                    );
                    templateRepository.save(newTemplate);
                    log.info("Imported new template [{}] for tenant [{}]", item.name(), tenantId);
                }
                count++;
            }

            return count;
        } finally {
            TenantContext.clear();
        }
    }

    private TemplateCategory parseCategory(String categoryStr) {
        if (categoryStr == null) return TemplateCategory.UTILITY;
        try {
            return TemplateCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TemplateCategory.UTILITY;
        }
    }

    private TemplateStatus parseStatus(String statusStr) {
        if (statusStr == null) return TemplateStatus.PENDING;
        try {
            return TemplateStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TemplateStatus.PENDING;
        }
    }
}
