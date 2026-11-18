package com.example.lpt.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lpt.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    MutableClock clock;
    LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-03-01T09:00:00Z"));
        limiter = new LoginRateLimiter(5, 15, clock);
    }

    @Test
    void blocksOnceTheLimitIsReached() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("email:a");
        }
        assertThat(limiter.isBlocked("email:a", 5)).isFalse();

        limiter.recordFailure("email:a");
        assertThat(limiter.isBlocked("email:a", 5)).isTrue();
    }

    @Test
    void failuresExpireAfterTheWindow() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("email:a");
        }
        clock.advance(Duration.ofMinutes(16));

        assertThat(limiter.isBlocked("email:a", 5)).isFalse();
    }

    @Test
    void windowSlidesInsteadOfResettingAllAtOnce() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure("email:a");
        }
        clock.advance(Duration.ofMinutes(10));
        limiter.recordFailure("email:a");
        limiter.recordFailure("email:a");
        assertThat(limiter.isBlocked("email:a", 5)).isTrue();

        clock.advance(Duration.ofMinutes(6)); // the first three are now older than 15 minutes
        assertThat(limiter.isBlocked("email:a", 5)).isFalse();
    }

    @Test
    void resetClearsTheKey() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("email:a");
        }
        limiter.reset("email:a");

        assertThat(limiter.isBlocked("email:a", 5)).isFalse();
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("email:a");
        }

        assertThat(limiter.isBlocked("email:a", 5)).isTrue();
        assertThat(limiter.isBlocked("email:b", 5)).isFalse();
        assertThat(limiter.isBlocked("ip:1.2.3.4", 20)).isFalse();
    }
}
