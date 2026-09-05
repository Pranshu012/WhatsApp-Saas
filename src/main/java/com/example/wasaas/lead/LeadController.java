package com.example.wasaas.lead;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public ResponseEntity<List<LeadService.LeadDto>> listLeads() {
        return ResponseEntity.ok(leadService.listLeads());
    }

    @PostMapping
    public ResponseEntity<LeadService.LeadDto> createLead(@Valid @RequestBody CreateLeadRequest request) {
        return ResponseEntity.ok(leadService.createManualLead(
                request.name(),
                request.phoneE164(),
                request.stage() != null ? request.stage() : LeadStage.NEW,
                request.dealValue(),
                request.notes()
        ));
    }

    @GetMapping("/analytics")
    public ResponseEntity<LeadService.LeadAnalyticsDto> getAnalytics() {
        return ResponseEntity.ok(leadService.getAnalytics());
    }

    @PatchMapping("/{id}/stage")
    public ResponseEntity<LeadService.LeadDto> updateStage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStageRequest request) {
        return ResponseEntity.ok(leadService.updateStage(id, request.stage()));
    }

    @PatchMapping("/{id}/details")
    public ResponseEntity<LeadService.LeadDto> updateDetails(
            @PathVariable UUID id,
            @RequestBody UpdateDetailsRequest request) {
        return ResponseEntity.ok(leadService.updateDetails(
                id,
                request.stage(),
                request.dealValue(),
                request.notes()
        ));
    }

    @PostMapping("/{id}/rescore")
    public ResponseEntity<LeadService.LeadDto> rescoreLead(@PathVariable UUID id) {
        return ResponseEntity.ok(leadService.rescoreLead(id));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<LeadQualificationRule>> listRules() {
        return ResponseEntity.ok(leadService.listRules());
    }

    @PostMapping("/rules")
    public ResponseEntity<LeadQualificationRule> createRule(@Valid @RequestBody CreateRuleRequest request) {
        return ResponseEntity.ok(leadService.createRule(
                request.ruleName(),
                request.matchKeywords(),
                request.scoreBoost()
        ));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        leadService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateLeadRequest(
            String name,
            @NotBlank String phoneE164,
            LeadStage stage,
            BigDecimal dealValue,
            String notes
    ) {}

    public record UpdateStageRequest(
            LeadStage stage
    ) {}

    public record UpdateDetailsRequest(
            LeadStage stage,
            BigDecimal dealValue,
            String notes
    ) {}

    public record CreateRuleRequest(
            @NotBlank String ruleName,
            @NotBlank String matchKeywords,
            int scoreBoost
    ) {}
}
