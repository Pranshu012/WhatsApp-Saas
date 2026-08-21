package com.example.wasaas.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tracks and enforces login rate limits.
 * 5 failures within 15 minutes per (email, IP) → blocked.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MINUTES = 15;

    private final LoginAttemptRepository repository;

    public LoginAttemptService(LoginAttemptRepository repository) {
        this.repository = repository;
    }

    public boolean isBlocked(String email, String ip) {
        Instant since = Instant.now().minus(WINDOW_MINUTES, ChronoUnit.MINUTES);
        return repository.countRecentFailures(email.toLowerCase(), ip, since) >= MAX_ATTEMPTS;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordFailure(String email, String ip) {
        repository.save(new LoginAttempt(email.toLowerCase(), ip));
    }

    @Transactional
    public void clearAttempts(String email, String ip) {
        repository.deleteByEmailAndIp(email.toLowerCase(), ip);
    }
}
