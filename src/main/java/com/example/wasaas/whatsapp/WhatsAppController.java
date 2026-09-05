package com.example.wasaas.whatsapp;

import jakarta.validation.Valid;
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
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WhatsAppController.class);

    private final WhatsAppConnectService connectService;
    private final WhatsAppAccountService accountService;
    private final WhatsAppAccountRepository accountRepository;
    private final com.example.wasaas.whatsapp.meta.MetaGraphClient metaGraphClient;
    private final com.example.wasaas.whatsapp.client.WhatsAppCloudClient whatsAppCloudClient;

    public WhatsAppController(WhatsAppConnectService connectService,
                              WhatsAppAccountService accountService,
                              WhatsAppAccountRepository accountRepository,
                              com.example.wasaas.whatsapp.meta.MetaGraphClient metaGraphClient,
                              com.example.wasaas.whatsapp.client.WhatsAppCloudClient whatsAppCloudClient) {
        this.connectService = connectService;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.metaGraphClient = metaGraphClient;
        this.whatsAppCloudClient = whatsAppCloudClient;
    }

    @PostMapping("/connect")
    public ResponseEntity<WhatsAppAccountResponse> connect(@Valid @RequestBody ConnectWhatsAppRequest request) {
        WhatsAppAccountResponse response = connectService.connect(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/connect-direct")
    public ResponseEntity<WhatsAppAccountResponse> connectDirect(@Valid @RequestBody SaveWhatsAppAccountCommand command) {
        try {
            log.info("Subscribing app to WABA [{}] webhooks...", command.wabaId());
            metaGraphClient.subscribeAppToWaba(command.wabaId(), command.rawAccessToken());
            log.info("Successfully subscribed app to WABA [{}] webhooks", command.wabaId());
        } catch (Exception e) {
            log.warn("Could not subscribe app to WABA [{}]: {}", command.wabaId(), e.getMessage());
        }
        WhatsAppAccount account = accountService.saveOrUpdateAccount(command);
        return ResponseEntity.ok(WhatsAppAccountResponse.from(account));
    }

    @GetMapping("/account")
    public WhatsAppAccountResponse getPrimaryAccount() {
        java.util.UUID tenantId = com.example.wasaas.tenant.context.TenantContext.require();
        WhatsAppAccount account = accountRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new com.example.wasaas.common.exception.DomainException(org.springframework.http.HttpStatus.NOT_FOUND, "WhatsApp account not connected for tenant"));
        return WhatsAppAccountResponse.from(account);
    }

    @PostMapping("/account/refresh")
    public WhatsAppAccountResponse refreshPrimaryAccount() {
        java.util.UUID tenantId = com.example.wasaas.tenant.context.TenantContext.require();
        WhatsAppAccount account = accountRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new com.example.wasaas.common.exception.DomainException(org.springframework.http.HttpStatus.NOT_FOUND, "WhatsApp account not connected for tenant"));
        WhatsAppAccount refreshed = accountService.refreshAccount(account.getId());
        return WhatsAppAccountResponse.from(refreshed);
    }

    @GetMapping("/accounts")
    public List<WhatsAppAccountResponse> listAccounts() {
        return accountRepository.findAll().stream()
                .map(WhatsAppAccountResponse::from)
                .toList();
    }

    @GetMapping("/accounts/{id}")
    public WhatsAppAccountResponse getAccount(@PathVariable UUID id) {
        WhatsAppAccount account = accountService.getAccount(id);
        return WhatsAppAccountResponse.from(account);
    }

    @PostMapping("/accounts/{id}/disconnect")
    public ResponseEntity<Void> disconnectAccount(@PathVariable UUID id) {
        accountService.disconnectAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-send")
    public ResponseEntity<TestMessageResponse> sendTestMessage(@Valid @RequestBody TestMessageRequest request) {
        UUID tenantId = com.example.wasaas.tenant.context.TenantContext.require();
        WhatsAppAccount account = accountRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new com.example.wasaas.common.exception.DomainException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "WhatsApp account not connected for tenant"));

        String decryptedToken = accountService.getDecryptedToken(account.getId());
        String businessName = account.getVerifiedName() != null && !account.getVerifiedName().isBlank()
                ? account.getVerifiedName()
                : "Your Business";
        String testText = "Hello! This is a live test message from " + businessName
                + " via WhatsApp SaaS. Your Meta Cloud API connection is active and operational!";

        try {
            com.example.wasaas.whatsapp.client.SendResult result = whatsAppCloudClient.sendText(
                    account.getPhoneNumberId(),
                    decryptedToken,
                    request.phoneE164(),
                    testText
            );
            return ResponseEntity.ok(new TestMessageResponse(true, "Test WhatsApp message sent successfully!", result.wamid()));
        } catch (com.example.wasaas.whatsapp.client.MetaTokenRevokedException e) {
            return ResponseEntity.ok(new TestMessageResponse(false, "Meta Access Token is expired or invalid (Error 190). Please click 'Update Credentials' to paste a new token.", null));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown delivery error";
            return ResponseEntity.ok(new TestMessageResponse(false, msg, null));
        }
    }

    public record TestMessageRequest(
            @jakarta.validation.constraints.NotBlank String phoneE164
    ) {}

    public record TestMessageResponse(
            boolean success,
            String message,
            String wamid
    ) {}
}
