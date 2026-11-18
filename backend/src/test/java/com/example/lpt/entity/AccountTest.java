package com.example.lpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final Instant NOW = Instant.parse("2026-03-01T09:00:00Z");

    private static Account account(boolean mustChange, Instant expiresAt) {
        return new Account("a@b.test", "hash", Role.CANDIDATE, mustChange, expiresAt);
    }

    @Test
    void temporaryPasswordIsValidBeforeItsExpiry() {
        assertThat(account(true, NOW.plusSeconds(1)).isTemporaryPasswordExpired(NOW)).isFalse();
    }

    @Test
    void temporaryPasswordExpiresAtTheExactExpiryInstant() {
        assertThat(account(true, NOW).isTemporaryPasswordExpired(NOW)).isTrue();
        assertThat(account(true, NOW.minusSeconds(1)).isTemporaryPasswordExpired(NOW)).isTrue();
    }

    @Test
    void permanentPasswordsNeverExpire() {
        assertThat(account(false, NOW.minusSeconds(86_400)).isTemporaryPasswordExpired(NOW)).isFalse();
        assertThat(account(false, null).isTemporaryPasswordExpired(NOW)).isFalse();
    }

    @Test
    void statusLabelsRoundTripAndRejectUnknownValues() {
        for (AssignmentStatus status : AssignmentStatus.values()) {
            assertThat(AssignmentStatus.fromLabel(status.label())).isEqualTo(status);
        }
        assertThat(AssignmentStatus.fromLabel(" in progress ")).isEqualTo(AssignmentStatus.IN_PROGRESS);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> AssignmentStatus.fromLabel("Done"))
                .isInstanceOf(com.example.lpt.exception.ApiException.class);
    }
}
