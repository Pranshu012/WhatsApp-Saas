package com.example.wasaas.ai;

import com.example.wasaas.tenant.Tenant;
import com.example.wasaas.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private final TenantAiConfigRepository aiConfigRepository;
    private final TenantRepository tenantRepository;
    private final GeminiClient geminiClient;
    private final com.example.wasaas.contact.ConversationMessageRepository conversationMessageRepository;

    public AiAssistantService(TenantAiConfigRepository aiConfigRepository,
                             TenantRepository tenantRepository,
                             GeminiClient geminiClient,
                             com.example.wasaas.contact.ConversationMessageRepository conversationMessageRepository) {
        this.aiConfigRepository = aiConfigRepository;
        this.tenantRepository = tenantRepository;
        this.geminiClient = geminiClient;
        this.conversationMessageRepository = conversationMessageRepository;
    }

    @Transactional(readOnly = true)
    public TenantAiConfig getConfig(UUID tenantId) {
        return aiConfigRepository.findById(tenantId).orElseGet(() -> {
            TenantAiConfig def = new TenantAiConfig(tenantId);
            tenantRepository.findById(tenantId).ifPresent(tenant -> {
                def.setBusinessContext("Business Name: " + tenant.getBusinessName() + "\n" +
                        "Location: Indiranagar, Bengaluru\n" +
                        "Timings: Monday - Saturday: 10:00 AM - 8:00 PM (Sunday Closed)\n" +
                        "Contact & Support: Reachable during working hours.");
            });
            return def;
        });
    }

    @Transactional
    public TenantAiConfig saveConfig(UUID tenantId, boolean enabled, String apiKey, String model,
                                    String businessContext, String systemPrompt, Integer monthlyLimit) {
        TenantAiConfig config = aiConfigRepository.findById(tenantId).orElse(new TenantAiConfig(tenantId));
        config.setEnabled(enabled);
        if (apiKey != null) {
            config.setApiKey(apiKey.trim());
        }
        if (model != null && !model.isBlank()) {
            config.setModel(model.trim());
        }
        if (businessContext != null) {
            config.setBusinessContext(businessContext.trim());
        }
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            config.setSystemPrompt(systemPrompt.trim());
        }
        if (monthlyLimit != null && monthlyLimit > 0) {
            config.setMonthlyLimit(monthlyLimit);
        }
        return aiConfigRepository.save(config);
    }

    @Transactional
    public Optional<String> generateReply(UUID tenantId, String userMessage) {
        return generateReply(tenantId, null, userMessage);
    }

    @Transactional
    public Optional<String> generateReply(UUID tenantId, UUID conversationId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }

        TenantAiConfig config = getConfig(tenantId);
        if (!config.isEnabled()) {
            log.debug("AI Assistant disabled for tenant [{}]", tenantId);
            return Optional.empty();
        }

        // Auto-reset on new calendar month
        String currentMonth = YearMonth.now().toString();
        if (!currentMonth.equals(config.getCurrentPeriodMonth())) {
            config.setCurrentPeriodMonth(currentMonth);
            config.setUsedThisMonth(0);
        }

        // Hard monthly quota check (Shield against token exhaustion)
        if (config.getUsedThisMonth() >= config.getMonthlyLimit()) {
            log.warn("AI Auto-reply suppressed for tenant [{}]: Monthly AI quota reached ({}/{}). Tokens protected.",
                    tenantId, config.getUsedThisMonth(), config.getMonthlyLimit());
            return Optional.empty();
        }

        if (!geminiClient.hasAvailableKey(config.getApiKey())) {
            log.warn("AI Assistant enabled for tenant [{}], but no Gemini API key is available", tenantId);
            return Optional.empty();
        }

        String businessContext = config.getBusinessContext();
        if (businessContext == null || businessContext.isBlank()) {
            Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
            businessContext = tenantOpt.map(t -> "Business Name: " + t.getBusinessName()).orElse("Business");
        }

        StringBuilder fullSystemPrompt = new StringBuilder();
        fullSystemPrompt.append(config.getSystemPrompt()).append("\n\n");
        fullSystemPrompt.append("--- APPROVED BUSINESS KNOWLEDGE & FACTS ---\n");
        fullSystemPrompt.append(businessContext).append("\n");
        fullSystemPrompt.append("--- END OF FACTS ---\n\n");
        fullSystemPrompt.append("Instructions:\n");
        fullSystemPrompt.append("1. Answer the customer directly and warmly based ONLY on the facts above.\n");
        fullSystemPrompt.append("2. Remember the ongoing conversation context. If the customer asks a follow-up question (e.g. 'timing kya hai', 'details', 'price'), answer in context of what was previously discussed.\n");
        fullSystemPrompt.append("3. Keep reply concise (under 2-3 sentences). Format for WhatsApp readability.\n");
        fullSystemPrompt.append("4. Match the customer's language (Hindi, English, or Hinglish).\n");
        fullSystemPrompt.append("5. If specific detail is not in the facts, politely offer to connect them to our team during working hours.\n");

        // Build sliding window conversation turns
        java.util.List<GeminiClient.ChatTurn> turns = new java.util.ArrayList<>();
        if (conversationId != null) {
            java.util.List<com.example.wasaas.contact.ConversationMessage> recent =
                    conversationMessageRepository.findTop8ByConversationIdOrderByCreatedAtDesc(conversationId);
            if (recent != null && !recent.isEmpty()) {
                // Reverse to chronological order (oldest to newest)
                java.util.List<com.example.wasaas.contact.ConversationMessage> chronological = new java.util.ArrayList<>(recent);
                java.util.Collections.reverse(chronological);

                // If the very latest message in DB is the current userMessage, skip it so we don't duplicate
                int size = chronological.size();
                if (size > 0) {
                    com.example.wasaas.contact.ConversationMessage lastInDb = chronological.get(size - 1);
                    if ("CUSTOMER".equalsIgnoreCase(lastInDb.getSenderType()) &&
                            userMessage.trim().equals(lastInDb.getTextContent().trim())) {
                        chronological.remove(size - 1);
                    }
                }

                for (com.example.wasaas.contact.ConversationMessage m : chronological) {
                    String role = "CUSTOMER".equalsIgnoreCase(m.getSenderType()) ? "user" : "model";
                    turns.add(new GeminiClient.ChatTurn(role, m.getTextContent()));
                }
            }
        }

        // Add the current incoming user message as the final turn
        turns.add(new GeminiClient.ChatTurn("user", userMessage.trim()));

        Optional<String> replyOpt = geminiClient.generateChatReply(
                config.getApiKey(),
                config.getModel(),
                fullSystemPrompt.toString(),
                turns
        );

        if (replyOpt.isPresent()) {
            config.incrementUsage();
            aiConfigRepository.save(config);
            log.info("Incremented AI usage for tenant [{}]: {}/{} (with {} conversation context turns)",
                    tenantId, config.getUsedThisMonth(), config.getMonthlyLimit(), turns.size());
        }

        return replyOpt;
    }

    @Transactional
    public String generateBroadcastMessage(UUID tenantId, String prompt, String tone) {
        TenantAiConfig config = getConfig(tenantId);
        String systemInstruction = """
            You are an elite WhatsApp marketing copywriter for small and medium business broadcasts.
            Write an engaging, high-converting, professional WhatsApp broadcast message based on the prompt.
            Rules:
            1. Keep it concise (under 90 words), highly readable on mobile screens.
            2. Personalize with {{name}} (e.g. 'Hello {{name}}!').
            3. Use 2-4 tasteful, relevant emojis suitable for WhatsApp.
            4. Include a clear, compelling Call-To-Action (e.g. 'Reply to this message to claim your discount').
            5. Include a brief opt-out line at the bottom: 'Reply STOP to unsubscribe'.
            6. Do not include markdown hashes (#), use *bold* or _italic_ for emphasis.
            7. Return ONLY the final WhatsApp message body directly. Do not include introductory notes, explanations, or quotes.
            """;

        String userQuery = "Campaign request: " + prompt + (tone != null && !tone.isBlank() ? " | Tone: " + tone : "");

        try {
            Optional<String> aiText = geminiClient.generateReply(config.getApiKey(), config.getModel(), systemInstruction, userQuery);
            if (aiText.isPresent() && !aiText.get().isBlank()) {
                config.incrementUsage();
                aiConfigRepository.save(config);
                return aiText.get().trim();
            }
        } catch (Exception e) {
            log.warn("Gemini template generation fallback due to: {}", e.getMessage());
        }

        return "Hello {{name}}! 👋\n\nWe have an exclusive special update for you regarding *" + prompt.trim() + "*.\n\nTake advantage of this limited-time opportunity today!\n\n👉 Reply directly to this message to get started or learn more.\n\n_Reply STOP to unsubscribe._";
    }
}
