package com.example.wasaas.ai;

import com.example.wasaas.tenant.context.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    public record AiConfigDto(
            boolean enabled,
            String apiKey,
            String model,
            String businessContext,
            String systemPrompt,
            int monthlyLimit,
            int usedThisMonth
    ) {
        public static AiConfigDto from(TenantAiConfig c) {
            String maskedKey = c.getApiKey();
            if (maskedKey != null && maskedKey.length() > 8) {
                maskedKey = maskedKey.substring(0, 4) + "••••••••" + maskedKey.substring(maskedKey.length() - 4);
            }
            return new AiConfigDto(
                    c.isEnabled(),
                    maskedKey,
                    c.getModel(),
                    c.getBusinessContext(),
                    c.getSystemPrompt(),
                    c.getMonthlyLimit(),
                    c.getUsedThisMonth()
            );
        }
    }

    public record UpdateAiConfigRequest(
            boolean enabled,
            String apiKey,
            String model,
            String businessContext,
            String systemPrompt,
            Integer monthlyLimit
    ) {}

    public record TestAiRequest(
            String message,
            UUID conversationId
    ) {}

    public record TestAiResponse(
            boolean success,
            String reply,
            String message
    ) {}

    @GetMapping("/config")
    public ResponseEntity<AiConfigDto> getConfig() {
        UUID tenantId = TenantContext.require();
        TenantAiConfig config = aiAssistantService.getConfig(tenantId);
        return ResponseEntity.ok(AiConfigDto.from(config));
    }

    @PutMapping("/config")
    public ResponseEntity<AiConfigDto> updateConfig(@Valid @RequestBody UpdateAiConfigRequest req) {
        UUID tenantId = TenantContext.require();
        TenantAiConfig current = aiAssistantService.getConfig(tenantId);

        String apiKeyToSave = req.apiKey();
        // If frontend sends masked key back unchanged, preserve original key
        if (apiKeyToSave != null && apiKeyToSave.contains("••••")) {
            apiKeyToSave = current.getApiKey();
        }

        TenantAiConfig saved = aiAssistantService.saveConfig(
                tenantId,
                req.enabled(),
                apiKeyToSave,
                req.model(),
                req.businessContext(),
                req.systemPrompt(),
                null // Monthly limit is controlled exclusively by Super Admin
        );
        return ResponseEntity.ok(AiConfigDto.from(saved));
    }

    @PostMapping("/test")
    public ResponseEntity<TestAiResponse> testAi(@RequestBody TestAiRequest req) {
        UUID tenantId = TenantContext.require();
        if (req.message() == null || req.message().isBlank()) {
            return ResponseEntity.badRequest().body(new TestAiResponse(false, null, "Message is empty."));
        }

        Optional<String> replyOpt = aiAssistantService.generateReply(tenantId, req.conversationId(), req.message());
        if (replyOpt.isPresent()) {
            return ResponseEntity.ok(new TestAiResponse(true, replyOpt.get(), "Generated via Gemini AI"));
        } else {
            return ResponseEntity.ok(new TestAiResponse(
                    false,
                    null,
                    "AI could not generate a reply. Please verify your Gemini API key and ensure AI is enabled."
            ));
        }
    }

    public record GenerateTemplateRequest(
            String prompt,
            String tone
    ) {}

    public record GenerateTemplateResponse(
            boolean success,
            String templateName,
            String messageText
    ) {}

    @PostMapping("/generate-template")
    public ResponseEntity<GenerateTemplateResponse> generateTemplate(@RequestBody GenerateTemplateRequest req) {
        UUID tenantId = TenantContext.require();
        String prompt = req.prompt() != null ? req.prompt().trim() : "Special Discount Promotion";
        String text = aiAssistantService.generateBroadcastMessage(tenantId, prompt, req.tone());
        String templateName = "promo_" + (System.currentTimeMillis() % 100000);
        return ResponseEntity.ok(new GenerateTemplateResponse(true, templateName, text));
    }
}
