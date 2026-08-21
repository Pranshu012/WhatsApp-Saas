package com.example.wasaas.automation.faq;

import java.util.UUID;

public record FaqMatchResult(
    UUID id,
    String question,
    String answer,
    double confidenceScore,
    boolean isConfident
) {
    public static FaqMatchResult noMatch() {
        return new FaqMatchResult(null, null, null, 0.0, false);
    }
}
