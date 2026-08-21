package com.example.wasaas.automation;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleMatcher {

    private final RegexValidator regexValidator;

    public RuleMatcher(RegexValidator regexValidator) {
        this.regexValidator = regexValidator;
    }

    public boolean matches(AutomationRule rule, String text) {
        if (rule == null || text == null) {
            return false;
        }

        String input = text.trim();
        String matchValue = rule.getMatchValue() != null ? rule.getMatchValue().trim() : "";
        boolean caseSensitive = rule.isCaseSensitive();

        return switch (rule.getMatchType()) {
            case EXACT -> caseSensitive ? input.equals(matchValue) : input.equalsIgnoreCase(matchValue);
            case CONTAINS -> caseSensitive
                    ? input.contains(matchValue)
                    : input.toLowerCase().contains(matchValue.toLowerCase());
            case STARTS_WITH -> caseSensitive
                    ? input.startsWith(matchValue)
                    : input.toLowerCase().startsWith(matchValue.toLowerCase());
            case REGEX -> matchRegex(matchValue, caseSensitive, input);
        };
    }

    private boolean matchRegex(String patternStr, boolean caseSensitive, String input) {
        try {
            Pattern pattern = regexValidator.validateAndCompile(patternStr, caseSensitive);
            Matcher matcher = pattern.matcher(input);
            return matcher.find();
        } catch (Exception e) {
            return false;
        }
    }
}
