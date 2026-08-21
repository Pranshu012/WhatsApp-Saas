package com.example.wasaas.scheduling;

import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.client.TemplateComponent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scheduled-messages")
public class ScheduledMessageController {

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final SchedulingService schedulingService;

    public ScheduledMessageController(ScheduledMessageRepository scheduledMessageRepository,
                                      SchedulingService schedulingService) {
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.schedulingService = schedulingService;
    }

    @GetMapping
    public ResponseEntity<List<ScheduledMessage>> listScheduledMessages() {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(scheduledMessageRepository.findAllByTenantId(tenantId));
    }

    @PostMapping
    public ResponseEntity<ScheduledMessage> scheduleMessage(@Valid @RequestBody ScheduleRequest request) {
        ScheduledMessage message = schedulingService.scheduleMessage(new ScheduleMessageCommand(
                request.contactId(),
                request.templateId(),
                request.whatsappAccountId(),
                request.components(),
                request.scheduledFor(),
                request.timezone()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelMessage(@PathVariable UUID id) {
        schedulingService.cancelScheduledMessage(id);
        return ResponseEntity.noContent().build();
    }

    public record ScheduleRequest(
            @NotNull UUID contactId,
            @NotNull UUID templateId,
            @NotNull UUID whatsappAccountId,
            List<TemplateComponent> components,
            @NotNull Instant scheduledFor,
            String timezone
    ) {}
}
