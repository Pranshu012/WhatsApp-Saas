package com.example.wasaas.whatsapp.send;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.job.Job;
import com.example.wasaas.job.JobHandler;
import com.example.wasaas.job.PermanentJobException;
import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.ledger.ConversationWindow;
import com.example.wasaas.ledger.LedgerService;
import com.example.wasaas.ledger.RecordOutboundIntentCommand;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountRepository;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.WhatsAppAccountStatus;
import com.example.wasaas.whatsapp.client.MetaTokenRevokedException;
import com.example.wasaas.whatsapp.client.SendResult;
import com.example.wasaas.whatsapp.client.WhatsAppCloudClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendMessageJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(SendMessageJobHandler.class);
    public static final String JOB_TYPE = "SEND_WHATSAPP_MESSAGE";

    private final WhatsAppAccountService accountService;
    private final WhatsAppAccountRepository accountRepository;
    private final WhatsAppCloudClient whatsAppCloudClient;
    private final LedgerService ledgerService;
    private final com.example.wasaas.template.TemplateService templateService;
    private final com.example.wasaas.subscription.SubscriptionService subscriptionService;
    private final AccountRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public SendMessageJobHandler(WhatsAppAccountService accountService,
                                 WhatsAppAccountRepository accountRepository,
                                 WhatsAppCloudClient whatsAppCloudClient,
                                 LedgerService ledgerService,
                                 com.example.wasaas.template.TemplateService templateService,
                                 com.example.wasaas.subscription.SubscriptionService subscriptionService,
                                 AccountRateLimiter rateLimiter,
                                 ObjectMapper objectMapper) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.whatsAppCloudClient = whatsAppCloudClient;
        this.ledgerService = ledgerService;
        this.templateService = templateService;
        this.subscriptionService = subscriptionService;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(Job job) throws Exception {
        SendMessageJobPayload payload = objectMapper.readValue(job.getPayload(), SendMessageJobPayload.class);

        WhatsAppAccount account = accountService.getAccount(payload.accountId());
        if (account.getStatus() == WhatsAppAccountStatus.DEAUTHORIZED || account.getStatus() == WhatsAppAccountStatus.DISCONNECTED) {
            throw new PermanentJobException("WhatsApp account [" + account.getId() + "] is not connected (status: " + account.getStatus() + ")");
        }

        if (!subscriptionService.isSubscriptionValid(account.getTenantId())) {
            throw new PermanentJobException("Outbound message blocked: Tenant [" + account.getTenantId() + "] subscription/trial is expired or suspended.");
        }

        BillingCategory billingCategory;
        if ("TEMPLATE".equalsIgnoreCase(payload.type())) {
            int paramCount = countTemplateParams(payload.components());
            com.example.wasaas.template.WhatsAppTemplate template = templateService.assertSendable(
                    account.getTenantId(), payload.templateName(), payload.languageCode(), paramCount);

            billingCategory = template.getCategory() != null
                    ? mapTemplateCategory(template.getCategory())
                    : (payload.billingCategory() != null ? payload.billingCategory() : BillingCategory.MARKETING);
        } else {
            billingCategory = payload.billingCategory() != null ? payload.billingCategory() : BillingCategory.SERVICE;
        }

        ConversationWindow conversationWindow = "TEMPLATE".equalsIgnoreCase(payload.type())
                ? ConversationWindow.OUT_OF_WINDOW
                : ConversationWindow.IN_WINDOW;

        // 1. Record Outbound Intent on the Ledger BEFORE the API call
        RecordOutboundIntentCommand intentCommand = new RecordOutboundIntentCommand(
                account.getId(),
                payload.toE164(),
                billingCategory,
                payload.templateName(),
                conversationWindow,
                payload.callerIdempotencyKey(),
                job.getId()
        );
        UUID ledgerId = ledgerService.recordOutboundIntent(intentCommand);

        // 2. Throttle
        rateLimiter.acquire(account.getPhoneNumberId());

        // 3. Decrypt token & call Meta Cloud API
        String decryptedToken = accountService.getDecryptedToken(account.getId());
        SendResult sendResult;

        try {
            if ("TEMPLATE".equalsIgnoreCase(payload.type())) {
                sendResult = whatsAppCloudClient.sendTemplate(
                        account.getPhoneNumberId(),
                        decryptedToken,
                        payload.toE164(),
                        payload.templateName(),
                        payload.languageCode(),
                        payload.components()
                );
            } else {
                sendResult = whatsAppCloudClient.sendText(
                        account.getPhoneNumberId(),
                        decryptedToken,
                        payload.toE164(),
                        payload.text()
                );
            }

            // 4. Attach wamid on success
            ledgerService.attachWamid(ledgerId, sendResult.wamid());
            log.info("Dispatched WhatsApp message for account [{}], ledger ID [{}], wamid [{}]",
                    account.getId(), ledgerId, sendResult.wamid());

        } catch (MetaTokenRevokedException e) {
            log.error("WhatsApp token revoked (Error 190) for account [{}]: {}", account.getId(), e.getMessage());
            account.setStatus(WhatsAppAccountStatus.DISCONNECTED);
            accountRepository.save(account);
            ledgerService.recordFailure(ledgerId, 190, e.getMessage());
            throw e;

        } catch (PermanentJobException e) {
            log.warn("Permanent failure sending message for account [{}]: {}", account.getId(), e.getMessage());
            ledgerService.recordFailure(ledgerId, 400, e.getMessage());
            throw e;

        } catch (DomainException e) {
            log.warn("Transient domain error sending message for account [{}]: {}", account.getId(), e.getMessage());
            ledgerService.recordFailure(ledgerId, e.status().value(), e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error sending message for account [{}]: {}", account.getId(), e.getMessage());
            ledgerService.recordFailure(ledgerId, 500, e.getMessage());
            throw e;
        }
    }

    private int countTemplateParams(java.util.List<com.example.wasaas.whatsapp.client.TemplateComponent> components) {
        if (components == null || components.isEmpty()) return 0;
        int count = 0;
        for (com.example.wasaas.whatsapp.client.TemplateComponent comp : components) {
            if (comp.parameters() != null) {
                count += comp.parameters().size();
            }
        }
        return count;
    }

    private BillingCategory mapTemplateCategory(com.example.wasaas.template.TemplateCategory category) {
        return switch (category) {
            case MARKETING -> BillingCategory.MARKETING;
            case UTILITY -> BillingCategory.UTILITY;
            case AUTHENTICATION -> BillingCategory.AUTHENTICATION;
        };
    }
}
