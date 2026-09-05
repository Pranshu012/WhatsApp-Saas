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
            case EXACT -> {
                if (matchValue.contains(",")) {
                    yield java.util.Arrays.stream(matchValue.split(","))
                            .map(String::trim)
                            .filter(t -> !t.isEmpty())
                            .anyMatch(t -> caseSensitive ? input.equals(t) : input.equalsIgnoreCase(t));
                }
                yield caseSensitive ? input.equals(matchValue) : input.equalsIgnoreCase(matchValue);
            }
            case CONTAINS -> {
                if (matchValue.contains(",")) {
                    yield java.util.Arrays.stream(matchValue.split(","))
                            .map(String::trim)
                            .filter(t -> !t.isEmpty())
                            .anyMatch(t -> matchContainsWord(t, input, caseSensitive));
                }
                yield matchContainsWord(matchValue, input, caseSensitive);
            }
            case STARTS_WITH -> {
                if (matchValue.contains(",")) {
                    yield java.util.Arrays.stream(matchValue.split(","))
                            .map(String::trim)
                            .filter(t -> !t.isEmpty())
                            .anyMatch(t -> caseSensitive ? input.startsWith(t) : input.toLowerCase().startsWith(t.toLowerCase()));
                }
                yield caseSensitive
                    ? input.startsWith(matchValue)
                    : input.toLowerCase().startsWith(matchValue.toLowerCase());
            }
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

    private boolean matchContainsWord(String target, String input, boolean caseSensitive) {
        if (target == null || target.isBlank()) {
            return false;
        }

        String trimmedTarget = target.trim();
        // Match discrete word boundary so "price" doesn't match inside "prices", "caprice", or "priceless"
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            Pattern p = Pattern.compile("\\b" + Pattern.quote(trimmedTarget) + "\\b", flags);
            return p.matcher(input).find();
        } catch (Exception e) {
            return caseSensitive ? input.contains(trimmedTarget) : input.toLowerCase().contains(trimmedTarget.toLowerCase());
        }
    }
}
