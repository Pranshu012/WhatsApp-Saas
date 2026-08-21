package com.example.wasaas.automation.faq;

import java.util.UUID;

public interface FaqMatchProjection {
    UUID getId();
    UUID getTenantId();
    String getQuestion();
    String getAnswer();
    boolean isEnabled();
    Double getTrgmScore();
    Double getTsScore();
    Double getCombinedScore();
}
