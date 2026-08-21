package com.example.wasaas.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantService;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    private final TenantService tenantService;
    public RegistrationController(TenantService tenantService) { this.tenantService = tenantService; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResponse register(@Valid @RequestBody RegistrationRequest request) {
        var result = tenantService.registerTenant(new RegistrationCommand(request.businessName(), request.slug(), request.fullName(), request.email(), request.password()));
        return new RegistrationResponse(result.businessName(), result.slug(), result.ownerName(), result.ownerEmail());
    }
}
