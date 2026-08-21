package com.example.wasaas.ledger;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final MessageLedgerRepository ledgerRepository;
    private final MessageLedgerStatusEventRepository statusEventRepository;

    public LedgerService(MessageLedgerRepository ledgerRepository,
                         MessageLedgerStatusEventRepository statusEventRepository) {
        this.ledgerRepository = ledgerRepository;
        this.statusEventRepository = statusEventRepository;
    }

    @Transactional
    public UUID recordOutboundIntent(RecordOutboundIntentCommand command) {
        UUID tenantId = TenantContext.require();

        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            Optional<MessageLedger> existing = ledgerRepository.findByIdempotencyKey(command.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Outbound intent with idempotency key [{}] already recorded as ledger ID [{}]",
                        command.idempotencyKey(), existing.get().getId());
                return existing.get().getId();
            }
        }

        String phoneHash = PhonePrivacyUtils.hashPhoneNumber(command.recipientPhoneNumber());
        String phoneLast4 = PhonePrivacyUtils.extractLast4(command.recipientPhoneNumber());

        MessageLedger ledger = new MessageLedger(
                command.whatsappAccountId(),
                MessageDirection.OUTBOUND,
                phoneHash,
                phoneLast4,
                command.billingCategory(),
                command.templateName(),
                command.conversationWindow(),
                MessageLedgerStatus.INTENT,
                command.idempotencyKey(),
                command.jobId()
        );
        ledger.setTenantId(tenantId);

        MessageLedger saved = ledgerRepository.save(ledger);
        log.debug("Recorded outbound intent for tenant [{}], ledger ID [{}]", tenantId, saved.getId());
        return saved.getId();
    }

    @Transactional
    public void attachWamid(UUID ledgerId, String wamid) {
        if (wamid == null || wamid.isBlank()) {
            throw new IllegalArgumentException("wamid cannot be empty");
        }

        MessageLedger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Message ledger entry not found for ID: " + ledgerId));

        ledger.setWamid(wamid);
        ledger.setStatus(MessageLedgerStatus.SENT);
        ledgerRepository.save(ledger);
        log.debug("Attached wamid [{}] to ledger ID [{}]", wamid, ledgerId);
    }

    @Transactional
    public void recordFailure(UUID ledgerId, Integer errorCode, String errorMessage) {
        MessageLedger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Message ledger entry not found for ID: " + ledgerId));

        ledger.setStatus(MessageLedgerStatus.FAILED);
        ledger.setErrorCode(errorCode);
        ledger.setErrorMessage(errorMessage);
        ledgerRepository.save(ledger);
        log.debug("Recorded failure on ledger ID [{}] with code [{}]", ledgerId, errorCode);
    }

    @Transactional
    public void recordStatusEvent(String wamid, MessageLedgerStatus status, String rawPayload) {
        if (wamid == null || wamid.isBlank()) {
            throw new IllegalArgumentException("wamid cannot be empty");
        }

        MessageLedger ledger = ledgerRepository.findByWamid(wamid)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Message ledger entry not found for wamid: " + wamid));

        UUID tenantId = ledger.getTenantId();

        MessageLedgerStatusEvent event = new MessageLedgerStatusEvent(
                ledger.getId(),
                status,
                Instant.now(),
                rawPayload
        );
        event.setTenantId(tenantId);
        statusEventRepository.save(event);

        ledger.setStatus(status);
        ledgerRepository.save(ledger);
        log.debug("Appended status event [{}] for ledger ID [{}], wamid [{}]", status, ledger.getId(), wamid);
    }

    @Transactional(readOnly = true)
    public Map<BillingCategory, Long> countByCategoryForMonth(UUID tenantId, YearMonth yearMonth) {
        Instant start = yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<BillingCategoryCount> counts = ledgerRepository.countByCategoryForDateRange(tenantId, start, end);

        Map<BillingCategory, Long> result = new EnumMap<>(BillingCategory.class);
        for (BillingCategory category : BillingCategory.values()) {
            result.put(category, 0L);
        }

        for (BillingCategoryCount count : counts) {
            result.put(count.getCategory(), count.getTotal());
        }

        return result;
    }
}
