package com.example.wasaas.template;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.job.JobService;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final WhatsAppTemplateRepository templateRepository;
    private final TemplateService templateService;
    private final WhatsAppAccountRepository accountRepository;
    private final JobService jobService;

    public TemplateController(WhatsAppTemplateRepository templateRepository,
                              TemplateService templateService,
                              WhatsAppAccountRepository accountRepository,
                              JobService jobService) {
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.accountRepository = accountRepository;
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<List<WhatsAppTemplate>> listTemplates() {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(templateRepository.findAllByTenantId(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WhatsAppTemplate> getTemplate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.require();
        WhatsAppTemplate template = templateRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Template not found: " + id));
        return ResponseEntity.ok(template);
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> syncTemplates() {
        UUID tenantId = TenantContext.require();
        WhatsAppAccount account = accountRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "WhatsApp account not connected for tenant"));

        String payload = String.format("{\"whatsappAccountId\": \"%s\", \"wabaId\": \"%s\"}", account.getId(), account.getWabaId());
        jobService.enqueue(tenantId, "SYNC_TEMPLATES", payload, "sync:tpl:" + tenantId + ":" + System.currentTimeMillis());
        return ResponseEntity.accepted().build();
    }

    @PostMapping
    public ResponseEntity<WhatsAppTemplate> submitTemplate(@Valid @RequestBody SubmitTemplateRequest request) {
        UUID tenantId = TenantContext.require();
        WhatsAppAccount account = accountRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "WhatsApp account not connected for tenant"));

        WhatsAppTemplate template = templateService.submitForApproval(new CreateTemplateCommand(
                account.getId(),
                request.name(),
                request.language(),
                request.category(),
                request.bodyText(),
                List.of()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    public record SubmitTemplateRequest(
            @NotBlank String name,
            @NotBlank String language,
            @NotNull TemplateCategory category,
            @NotBlank String bodyText,
            String headerText,
            String footerText
    ) {}
}
