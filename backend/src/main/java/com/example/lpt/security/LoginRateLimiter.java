package com.example.lpt.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simple in-memory sliding-window limiter for FAILED logins. Keys are opaque strings
 * (e.g. "email:x" and "ip:y"). Unknown emails are tracked exactly like known ones, so
 * the limiter reveals nothing about which accounts exist. Resets on restart (fine for this scope).
 */
@Component
public class LoginRateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginRateLimiter(@Value("${app.login.max-attempts:5}") int maxAttempts,
                            @Value("${app.login.window-minutes:15}") long windowMinutes,
                            Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
        this.clock = clock;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public boolean isBlocked(String key, int limit) {
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            prune(attempts);
            return attempts.size() >= limit;
        }
    }

    public void recordFailure(String key) {
        if (failures.size() > CLEANUP_THRESHOLD) {
            failures.values().removeIf(d -> {
                synchronized (d) {
                    prune(d);
                    return d.isEmpty();
                }
            });
        }
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(clock.instant());
        }
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> attempts) {
        Instant cutoff = clock.instant().minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.removeFirst();
        }
    }
}
