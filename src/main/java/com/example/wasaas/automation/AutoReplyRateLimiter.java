package com.example.wasaas.automation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AutoReplyRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AutoReplyRateLimiter.class);

    private final int maxRepliesPerHour;
    private final Map<String, Deque<Instant>> replyHistory = new ConcurrentHashMap<>();

    public AutoReplyRateLimiter(@Value("${app.automation.max-replies-per-contact-per-hour:5}") int maxRepliesPerHour) {
        this.maxRepliesPerHour = maxRepliesPerHour;
    }

    public synchronized boolean tryAcquire(UUID tenantId, String contactPhone) {
        if (tenantId == null || contactPhone == null || contactPhone.isBlank()) {
            return false;
        }

        String key = tenantId + ":" + contactPhone.trim();
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);

        Deque<Instant> timestamps = replyHistory.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Evict timestamps older than 1 hour
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(oneHourAgo)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRepliesPerHour) {
            log.warn("Auto-reply rate limit reached for contact [{}] under tenant [{}] (limit: {}/hour)",
                    contactPhone, tenantId, maxRepliesPerHour);
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    public void reset() {
        replyHistory.clear();
    }
}
