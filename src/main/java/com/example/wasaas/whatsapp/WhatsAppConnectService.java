package com.example.wasaas.whatsapp;

import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.meta.MetaGraphClient;
import com.example.wasaas.whatsapp.meta.MetaPhoneNumberDetails;
import com.example.wasaas.whatsapp.meta.MetaWabaDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WhatsAppConnectService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppConnectService.class);

    private final MetaGraphClient metaGraphClient;
    private final WhatsAppAccountService accountService;
    private final com.example.wasaas.job.JobService jobService;

    public WhatsAppConnectService(MetaGraphClient metaGraphClient,
                                  WhatsAppAccountService accountService,
                                  com.example.wasaas.job.JobService jobService) {
        this.metaGraphClient = metaGraphClient;
        this.accountService = accountService;
        this.jobService = jobService;
    }

    @Transactional
    public WhatsAppAccountResponse connect(ConnectWhatsAppRequest request) {
        UUID tenantId = TenantContext.require();

        // 1. Exchange code -> Business Access Token
        String accessToken = metaGraphClient.exchangeCodeForToken(request.code());

        // 2. Verify WABA and Phone Number ownership against Meta Graph API using the token
        MetaWabaDetails waba = metaGraphClient.getWabaDetails(request.wabaId(), accessToken);
        MetaPhoneNumberDetails phone = metaGraphClient.getPhoneNumberDetails(request.phoneNumberId(), accessToken);

        // 3. Subscribe our app to this WABA for incoming webhooks
        metaGraphClient.subscribeAppToWaba(request.wabaId(), accessToken);

        // 4. Save/Update account with AES-256-GCM encrypted token
        SaveWhatsAppAccountCommand saveCommand = new SaveWhatsAppAccountCommand(
                request.wabaId(),
                request.phoneNumberId(),
                phone.displayPhoneNumber(),
                phone.verifiedName() != null && !phone.verifiedName().isBlank() ? phone.verifiedName() : waba.name(),
                phone.qualityRating(),
                phone.messagingLimitTier(),
                accessToken
        );

        WhatsAppAccount account = accountService.saveOrUpdateAccount(saveCommand);

        // 5. Automatically trigger template synchronization for newly connected account
        String syncPayload = String.format("{\"whatsappAccountId\": \"%s\", \"wabaId\": \"%s\"}", account.getId(), account.getWabaId());
        jobService.enqueue(tenantId, "SYNC_TEMPLATES", syncPayload, "initial-sync:tpl:" + account.getId());

        log.info("Successfully connected WhatsApp account for tenant [{}], WABA [{}], Phone Number ID [{}]",
                tenantId, request.wabaId(), request.phoneNumberId());

        return WhatsAppAccountResponse.from(account);
    }
}
