package com.example.wasaas.tenant;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.context.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings/business")
public class TenantSettingsController {

    private final TenantRepository tenantRepository;

    public TenantSettingsController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public record BusinessSettingsDto(
            UUID id,
            String businessName,
            String slug,
            String timezone,
            String gstin,
            String legalName,
            String billingAddress
    ) {
        public static BusinessSettingsDto from(Tenant tenant) {
            return new BusinessSettingsDto(
                    tenant.getId(),
                    tenant.getBusinessName(),
                    tenant.getSlug(),
                    tenant.getTimezone(),
                    tenant.getGstin(),
                    tenant.getLegalName(),
                    tenant.getBillingAddress()
            );
        }
    }

    public record UpdateBusinessSettingsRequest(
            @NotBlank String businessName,
            String timezone,
            String gstin,
            String legalName,
            String billingAddress
    ) {}

    @GetMapping
    public ResponseEntity<BusinessSettingsDto> getSettings() {
        UUID tenantId = TenantContext.require();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Tenant not found: " + tenantId));
        return ResponseEntity.ok(BusinessSettingsDto.from(tenant));
    }

    @PutMapping
    public ResponseEntity<BusinessSettingsDto> updateSettings(@Valid @RequestBody UpdateBusinessSettingsRequest request) {
        UUID tenantId = TenantContext.require();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Tenant not found: " + tenantId));

        tenant.setBusinessName(request.businessName().trim());
        if (request.timezone() != null && !request.timezone().isBlank()) {
            tenant.setTimezone(request.timezone().trim());
        }
        tenant.setGstin(request.gstin() != null && !request.gstin().isBlank() ? request.gstin().trim().toUpperCase() : null);
        tenant.setLegalName(request.legalName() != null && !request.legalName().isBlank() ? request.legalName().trim() : null);
        tenant.setBillingAddress(request.billingAddress() != null && !request.billingAddress().isBlank() ? request.billingAddress().trim() : null);

        Tenant saved = tenantRepository.save(tenant);
        return ResponseEntity.ok(BusinessSettingsDto.from(saved));
    }
}
