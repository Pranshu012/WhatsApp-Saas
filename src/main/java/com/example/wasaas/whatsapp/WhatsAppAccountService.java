package com.example.wasaas.whatsapp;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.common.exception.NotFoundException;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.crypto.TokenCipher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class WhatsAppAccountService {

    private final WhatsAppAccountRepository repository;
    private final TokenCipher tokenCipher;

    private final com.example.wasaas.whatsapp.meta.MetaGraphClient metaGraphClient;

    public WhatsAppAccountService(WhatsAppAccountRepository repository,
                                  TokenCipher tokenCipher,
                                  com.example.wasaas.whatsapp.meta.MetaGraphClient metaGraphClient) {
        this.repository = repository;
        this.tokenCipher = tokenCipher;
        this.metaGraphClient = metaGraphClient;
    }

    @Transactional
    public WhatsAppAccount refreshAccount(UUID accountId) {
        WhatsAppAccount account = repository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("WhatsApp account not found"));

        String token = tokenCipher.decrypt(account.getAccessTokenEncrypted());
        com.example.wasaas.whatsapp.meta.MetaPhoneNumberDetails phone =
                metaGraphClient.getPhoneNumberDetails(account.getPhoneNumberId(), token);

        account.updateDetails(
                phone.displayPhoneNumber(),
                phone.verifiedName() != null && !phone.verifiedName().isBlank() ? phone.verifiedName() : account.getVerifiedName(),
                phone.qualityRating(),
                phone.messagingLimitTier()
        );
        return repository.save(account);
    }

    @Transactional
    public WhatsAppAccount saveOrUpdateAccount(SaveWhatsAppAccountCommand command) {
        UUID tenantId = TenantContext.require();
        byte[] encryptedToken = tokenCipher.encrypt(command.rawAccessToken());

        Optional<WhatsAppAccount> existingOpt = repository.findByPhoneNumberId(command.phoneNumberId());
        if (existingOpt.isPresent()) {
            WhatsAppAccount account = existingOpt.get();
            account.updateToken(encryptedToken);
            account.updateDetails(
                    command.displayPhoneNumber(),
                    command.verifiedName(),
                    command.qualityRating(),
                    command.messagingLimitTier()
            );
            return repository.save(account);
        }

        WhatsAppAccount newAccount = new WhatsAppAccount(
                tenantId,
                command.wabaId(),
                command.phoneNumberId(),
                command.displayPhoneNumber(),
                command.verifiedName(),
                command.qualityRating(),
                command.messagingLimitTier(),
                encryptedToken
        );
        return repository.save(newAccount);
    }

    @Transactional(readOnly = true)
    public String getDecryptedToken(UUID accountId) {
        WhatsAppAccount account = repository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("WhatsApp account not found"));
        return tokenCipher.decrypt(account.getAccessTokenEncrypted());
    }

    @Transactional(readOnly = true)
    public WhatsAppAccount getAccount(UUID accountId) {
        return repository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("WhatsApp account not found"));
    }

    @Transactional
    public void disconnectAccount(UUID accountId) {
        WhatsAppAccount account = repository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("WhatsApp account not found"));
        account.disconnect();
        repository.save(account);
    }
}
