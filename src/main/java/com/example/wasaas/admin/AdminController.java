package com.example.wasaas.admin;

import com.example.wasaas.auth.TenantPrincipal;
import com.example.wasaas.common.exception.DomainException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public AdminPlatformStatsDto getStats() {
        return adminService.getPlatformStats();
    }

    @GetMapping("/tenants")
    public List<AdminTenantDto> getAllTenants() {
        return adminService.getAllTenants();
    }

    @GetMapping("/tenants/{id}")
    public AdminTenantDto getTenantDetails(@PathVariable("id") UUID id) {
        return adminService.getTenantDetails(id);
    }

    @PostMapping("/tenants/{id}/activate")
    public AdminTenantDto activateTenant(@PathVariable("id") UUID id,
                                         @Valid @RequestBody ActivateTenantRequest request) {
        return adminService.activateTenant(id, request);
    }

    @PostMapping("/tenants/{id}/extend")
    public AdminTenantDto extendTenantSubscription(@PathVariable("id") UUID id,
                                                   @Valid @RequestBody ExtendSubscriptionRequest request) {
        return adminService.extendTenantSubscription(id, request);
    }

    @PostMapping("/tenants/{id}/suspend")
    public AdminTenantDto suspendTenant(@PathVariable("id") UUID id,
                                        @RequestBody(required = false) SuspendTenantRequest request) {
        return adminService.suspendTenant(id, request);
    }
}
