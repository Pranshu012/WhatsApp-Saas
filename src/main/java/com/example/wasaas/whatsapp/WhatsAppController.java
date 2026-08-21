package com.example.wasaas.whatsapp;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final WhatsAppConnectService connectService;
    private final WhatsAppAccountService accountService;
    private final WhatsAppAccountRepository accountRepository;

    public WhatsAppController(WhatsAppConnectService connectService,
                              WhatsAppAccountService accountService,
                              WhatsAppAccountRepository accountRepository) {
        this.connectService = connectService;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/connect")
    public ResponseEntity<WhatsAppAccountResponse> connect(@Valid @RequestBody ConnectWhatsAppRequest request) {
        WhatsAppAccountResponse response = connectService.connect(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts")
    public List<WhatsAppAccountResponse> listAccounts() {
        return accountRepository.findAll().stream()
                .map(WhatsAppAccountResponse::from)
                .toList();
    }

    @GetMapping("/accounts/{id}")
    public WhatsAppAccountResponse getAccount(@PathVariable UUID id) {
        WhatsAppAccount account = accountService.getAccount(id);
        return WhatsAppAccountResponse.from(account);
    }

    @PostMapping("/accounts/{id}/disconnect")
    public ResponseEntity<Void> disconnectAccount(@PathVariable UUID id) {
        accountService.disconnectAccount(id);
        return ResponseEntity.noContent().build();
    }
}
