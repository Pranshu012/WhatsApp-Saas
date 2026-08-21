package com.example.wasaas.automation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConsolidationValidator {

    public Optional<String> validateActionConsolidation(JsonNode actionPayload) {
        if (actionPayload == null) {
            return Optional.empty();
        }

        if (actionPayload.has("messages") && actionPayload.get("messages").isArray()) {
            int count = actionPayload.get("messages").size();
            if (count > 1) {
                return Optional.of("Warning: Action defines " + count + " separate messages. Under Meta October 2026 rules, each message is billed individually (~Rs 0.115/msg in India). Consolidating into a single message saves " + (count - 1) + "x billing cost.");
            }
        }

        return Optional.empty();
    }
}
