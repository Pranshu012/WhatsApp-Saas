package com.example.wasaas.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String systemApiKey;
    private final String defaultModel;

    public GeminiClient(ObjectMapper objectMapper,
                        RestClient.Builder restClientBuilder,
                        @Value("${app.gemini.api-key:}") String systemApiKey,
                        @Value("${app.gemini.default-model:gemini-3.5-flash-lite}") String defaultModel) {
        this.objectMapper = objectMapper;
        this.systemApiKey = systemApiKey;
        this.defaultModel = defaultModel != null && !defaultModel.isBlank() ? defaultModel : "gemini-3.5-flash-lite";

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(6000);

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    public record ChatTurn(String role, String text) {}

    public Optional<String> generateReply(String explicitApiKey, String modelOverride, String systemInstruction, String userQuery) {
        return generateChatReply(explicitApiKey, modelOverride, systemInstruction, List.of(new ChatTurn("user", userQuery)));
    }

    public Optional<String> generateChatReply(String explicitApiKey, String modelOverride, String systemInstruction, List<ChatTurn> turns) {
        String apiKey = (explicitApiKey != null && !explicitApiKey.isBlank()) ? explicitApiKey.trim() : systemApiKey;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Cannot call Gemini: No API key configured (neither tenant key nor system key)");
            return Optional.empty();
        }

        String model = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride.trim() : defaultModel;
        if (model.contains("1.5") || model.contains("2.5") || "gemini-flash".equals(model) || "gemini-flash-latest".equals(model)) {
            if (model.contains("pro")) {
                model = "gemini-pro-latest";
            } else {
                model = "gemini-3.5-flash-lite";
            }
        }

        List<Map<String, Object>> contents = buildGeminiContents(turns);

        // 1. Try primary model (sub-second gemini-3.5-flash-lite)
        Optional<String> primaryRes = callGemini(model, apiKey, systemInstruction, contents);
        if (primaryRes.isPresent()) {
            return primaryRes;
        }

        // 2. Automatic fallback to gemini-3.6-flash if primary encounters 503 or error
        if (!"gemini-3.6-flash".equals(model)) {
            log.info("Retrying with fallback model [gemini-3.6-flash] with {} turns", turns != null ? turns.size() : 0);
            return callGemini("gemini-3.6-flash", apiKey, systemInstruction, contents);
        }

        return Optional.empty();
    }

    private List<Map<String, Object>> buildGeminiContents(List<ChatTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return List.of(Map.of("role", "user", "parts", List.of(Map.of("text", ""))));
        }

        List<ChatTurn> cleaned = new ArrayList<>();
        for (ChatTurn t : turns) {
            if (t == null || t.text() == null || t.text().isBlank()) continue;
            String role = "model".equalsIgnoreCase(t.role()) ? "model" : "user";
            if (!cleaned.isEmpty() && cleaned.get(cleaned.size() - 1).role().equals(role)) {
                // Merge consecutive same-role turns
                ChatTurn prev = cleaned.remove(cleaned.size() - 1);
                cleaned.add(new ChatTurn(role, prev.text() + "\n" + t.text().trim()));
            } else {
                cleaned.add(new ChatTurn(role, t.text().trim()));
            }
        }

        // Gemini requires first turn to be 'user'
        while (!cleaned.isEmpty() && !"user".equals(cleaned.get(0).role())) {
            cleaned.remove(0);
        }

        // Gemini requires last turn to be 'user'
        while (!cleaned.isEmpty() && !"user".equals(cleaned.get(cleaned.size() - 1).role())) {
            cleaned.remove(cleaned.size() - 1);
        }

        if (cleaned.isEmpty()) {
            return List.of(Map.of("role", "user", "parts", List.of(Map.of("text", ""))));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatTurn t : cleaned) {
            result.add(Map.of(
                    "role", t.role(),
                    "parts", List.of(Map.of("text", t.text()))
            ));
        }
        return result;
    }

    private Optional<String> callGemini(String model, String apiKey, String systemInstruction, List<Map<String, Object>> contents) {
        String uri = String.format("%s/%s:generateContent?key=%s", GEMINI_API_BASE_URL, model, apiKey);

        try {
            Map<String, Object> systemInstructionMap = null;
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                systemInstructionMap = Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                );
            }

            Map<String, Object> payload;
            if (systemInstructionMap != null) {
                payload = Map.of(
                        "contents", contents,
                        "system_instruction", systemInstructionMap,
                        "generationConfig", Map.of(
                                "temperature", 0.3,
                                "maxOutputTokens", 200
                        )
                );
            } else {
                payload = Map.of(
                        "contents", contents,
                        "generationConfig", Map.of(
                                "temperature", 0.3,
                                "maxOutputTokens", 200
                        )
                );
            }

            String responseJson = restClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.isBlank()) {
                log.warn("Gemini returned empty response body for model [{}]", model);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                if (content != null && content.has("parts")) {
                    JsonNode parts = content.get("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        String replyText = parts.get(0).path("text").asText("");
                        return Optional.of(replyText.trim());
                    }
                }
            }

            log.warn("Gemini response missing candidate parts for model [{}]: {}", model, responseJson);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Gemini API call failed for model [{}]: {}", model, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean hasAvailableKey(String tenantApiKey) {
        return (tenantApiKey != null && !tenantApiKey.isBlank()) || (systemApiKey != null && !systemApiKey.isBlank());
    }
}
