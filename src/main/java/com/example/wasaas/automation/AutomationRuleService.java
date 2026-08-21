package com.example.wasaas.automation;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AutomationRuleService {

    private static final Logger log = LoggerFactory.getLogger(AutomationRuleService.class);

    private final AutomationRuleRepository ruleRepository;
    private final RegexValidator regexValidator;

    public AutomationRuleService(AutomationRuleRepository ruleRepository, RegexValidator regexValidator) {
        this.ruleRepository = ruleRepository;
        this.regexValidator = regexValidator;
    }

    @Transactional
    public AutomationRule createRule(CreateRuleCommand command) {
        UUID tenantId = TenantContext.require();

        if (command.name() == null || command.name().isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Rule name cannot be empty");
        }
        if (command.matchValue() == null || command.matchValue().isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Match value cannot be empty");
        }
        if (command.actionPayload() == null || command.actionPayload().isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Action payload cannot be empty");
        }

        // If REGEX, validate at save time to catch syntax and ReDoS errors
        if (command.matchType() == MatchType.REGEX) {
            regexValidator.validateAndCompile(command.matchValue(), command.caseSensitive());
        }

        AutomationRule rule = new AutomationRule(
                tenantId,
                command.name().trim(),
                command.enabled(),
                command.matchType(),
                command.matchValue().trim(),
                command.caseSensitive(),
                command.priority(),
                command.actionType(),
                command.actionPayload().trim()
        );

        AutomationRule saved = ruleRepository.save(rule);
        log.info("Created automation rule [{}] (type={}, priority={}) for tenant [{}]",
                saved.getName(), saved.getMatchType(), saved.getPriority(), tenantId);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AutomationRule> getActiveRules(UUID tenantId) {
        return ruleRepository.findAllByTenantIdAndEnabledTrueOrderByPriorityAsc(tenantId);
    }
}
