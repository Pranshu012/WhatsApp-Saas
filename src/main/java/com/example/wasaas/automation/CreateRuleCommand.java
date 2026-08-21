package com.example.wasaas.automation;

public record CreateRuleCommand(
    String name,
    boolean enabled,
    MatchType matchType,
    String matchValue,
    boolean caseSensitive,
    int priority,
    ActionType actionType,
    String actionPayload
) {}
