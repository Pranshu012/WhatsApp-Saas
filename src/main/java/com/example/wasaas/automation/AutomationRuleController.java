package com.example.wasaas.automation;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.context.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/automation-rules")
public class AutomationRuleController {

    private final AutomationRuleRepository ruleRepository;
    private final AutomationRuleService ruleService;
    private final RuleMatcher ruleMatcher;
    private final RegexValidator regexValidator;

    public AutomationRuleController(AutomationRuleRepository ruleRepository,
                                    AutomationRuleService ruleService,
                                    RuleMatcher ruleMatcher,
                                    RegexValidator regexValidator) {
        this.ruleRepository = ruleRepository;
        this.ruleService = ruleService;
        this.ruleMatcher = ruleMatcher;
        this.regexValidator = regexValidator;
    }

    @GetMapping
    public ResponseEntity<List<AutomationRule>> listRules() {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(ruleRepository.findAllByTenantIdOrderByPriorityAsc(tenantId));
    }

    @PostMapping
    public ResponseEntity<AutomationRule> createRule(@Valid @RequestBody CreateRuleDto dto) {
        AutomationRule rule = ruleService.createRule(new CreateRuleCommand(
                dto.name(),
                dto.enabled() != null ? dto.enabled() : true,
                dto.matchType(),
                dto.matchValue(),
                dto.caseSensitive() != null ? dto.caseSensitive() : false,
                dto.priority() != null ? dto.priority() : 100,
                dto.actionType(),
                dto.actionPayload()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AutomationRule> updateRule(@PathVariable UUID id, @Valid @RequestBody CreateRuleDto dto) {
        UUID tenantId = TenantContext.require();
        AutomationRule rule = ruleRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Automation rule not found: " + id));

        if (dto.matchType() == MatchType.REGEX) {
            regexValidator.validateAndCompile(dto.matchValue(), dto.caseSensitive() != null && dto.caseSensitive());
        }

        rule.updateDetails(
                dto.name(),
                dto.enabled() != null ? dto.enabled() : true,
                dto.matchType(),
                dto.matchValue(),
                dto.caseSensitive() != null ? dto.caseSensitive() : false,
                dto.priority() != null ? dto.priority() : 100,
                dto.actionType(),
                dto.actionPayload()
        );

        AutomationRule saved = ruleRepository.save(rule);
        return ResponseEntity.ok(saved);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/toggle")
    public ResponseEntity<AutomationRule> toggleRule(@PathVariable UUID id) {
        UUID tenantId = TenantContext.require();
        AutomationRule rule = ruleRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Automation rule not found: " + id));

        rule.setEnabled(!rule.isEnabled());
        AutomationRule saved = ruleRepository.save(rule);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        UUID tenantId = TenantContext.require();
        AutomationRule rule = ruleRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Automation rule not found: " + id));
        ruleRepository.delete(rule);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<TestRuleResponse> testRule(@RequestBody Map<String, String> request) {
        UUID tenantId = TenantContext.require();
        String messageText = request.get("messageText");
        if (messageText == null || messageText.isBlank()) {
            return ResponseEntity.ok(new TestRuleResponse(false, null, null));
        }

        List<AutomationRule> rules = ruleRepository.findAllByTenantIdAndEnabledTrueOrderByPriorityAsc(tenantId);
        for (AutomationRule rule : rules) {
            if (ruleMatcher.matches(rule, messageText)) {
                return ResponseEntity.ok(new TestRuleResponse(true, rule.getId(), rule.getName()));
            }
        }

        return ResponseEntity.ok(new TestRuleResponse(false, null, null));
    }

    public record CreateRuleDto(
            @NotBlank String name,
            Boolean enabled,
            @NotNull MatchType matchType,
            @NotBlank String matchValue,
            Boolean caseSensitive,
            Integer priority,
            @NotNull ActionType actionType,
            @NotBlank String actionPayload
    ) {}

    public record TestRuleResponse(
            boolean matched,
            UUID ruleId,
            String ruleName
    ) {}
}
