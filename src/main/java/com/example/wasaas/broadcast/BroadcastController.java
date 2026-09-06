package com.example.wasaas.broadcast;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/broadcasts")
public class BroadcastController {

    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @GetMapping
    public ResponseEntity<List<BroadcastService.BroadcastCampaignDto>> listCampaigns() {
        return ResponseEntity.ok(broadcastService.listCampaigns());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BroadcastService.BroadcastCampaignDetailDto> getCampaign(@PathVariable UUID id) {
        return ResponseEntity.ok(broadcastService.getCampaign(id));
    }

    @PostMapping
    public ResponseEntity<BroadcastService.BroadcastCampaignDto> createCampaign(
            @Valid @RequestBody BroadcastService.CreateBroadcastRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(broadcastService.createCampaign(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelCampaign(@PathVariable UUID id) {
        broadcastService.cancelCampaign(id);
        return ResponseEntity.noContent().build();
    }
}
