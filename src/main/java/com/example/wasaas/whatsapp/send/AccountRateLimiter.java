package com.example.wasaas.whatsapp.send;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AccountRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AccountRateLimiter.class);

    // TODO(scale): in-process only — needs Redis when we run multiple workers.
    // See 12-SCALING/WHEN-TO-INTRODUCE-REDIS.md. Do NOT add Redis now.
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private static final int MAX_BURST_PER_SEC = 50;

    public void acquire(String phoneNumberId) {
        TokenBucket bucket = buckets.computeIfAbsent(phoneNumberId, k -> new TokenBucket(MAX_BURST_PER_SEC));
        bucket.acquire();
    }

    private static class TokenBucket {
        private final int capacity;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;

        public TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        }

        public void acquire() {
            refill();
            int current = tokens.getAndUpdate(t -> t > 0 ? t - 1 : 0);
            if (current <= 0) {
                try {
                    // Small yield sleep if burst limit is reached
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long last = lastRefillTime.get();
            if (now - last > 1000) {
                if (lastRefillTime.compareAndSet(last, now)) {
                    tokens.set(capacity);
                }
            }
        }
    }
}
