package com.example.wasaas.automation;

import com.example.wasaas.common.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class RegexValidator {

    private static final int MAX_REGEX_LENGTH = 500;
    private static final long COMPILE_TIMEOUT_MS = 50;

    // Common ReDoS patterns: nested repetition/quantifiers like (a+)+, (a*)*, (a|b)+, etc.
    private static final Pattern NESTED_QUANTIFIERS = Pattern.compile(
            "\\([^)]*[+*]\\)[+*]|\\([^)]*\\|[+*]\\)[+*]|\\(\\.[*+]\\)[+*]|\\(\\w\\+\\)\\+"
    );

    public Pattern validateAndCompile(String regex, boolean caseSensitive) {
        if (regex == null || regex.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Regex pattern cannot be empty");
        }

        if (regex.length() > MAX_REGEX_LENGTH) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Regex pattern exceeds maximum length of " + MAX_REGEX_LENGTH + " characters");
        }

        // 1. Structural check for classic catastrophic backtracking patterns
        if (NESTED_QUANTIFIERS.matcher(regex).find()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Unsafe regex pattern detected: nested quantifiers may cause catastrophic backtracking (ReDoS)");
        }

        // 2. Syntax validation
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern compiled;
        try {
            compiled = Pattern.compile(regex, flags);
        } catch (PatternSyntaxException e) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Invalid regular expression syntax: " + e.getDescription());
        }

        // 3. Execution test with timeout against benchmark test strings
        testAgainstBenchmark(compiled);

        return compiled;
    }

    private void testAgainstBenchmark(Pattern pattern) {
        String testInput = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaab"; // Classic adversarial test for exponential backtracking

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> pattern.matcher(testInput).find());

        try {
            future.get(COMPILE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new DomainException(HttpStatus.BAD_REQUEST, "Regex evaluation timed out: pattern exhibits catastrophic backtracking");
        } catch (ExecutionException e) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Regex evaluation error: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "Regex validation interrupted");
        }
    }
}
