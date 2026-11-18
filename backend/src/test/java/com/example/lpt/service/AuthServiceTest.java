package com.example.lpt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.lpt.dto.ChangePasswordRequest;
import com.example.lpt.dto.LoginRequest;
import com.example.lpt.dto.LoginResponse;
import com.example.lpt.entity.Account;
import com.example.lpt.entity.Role;
import com.example.lpt.exception.ApiException;
import com.example.lpt.repository.AccountRepository;
import com.example.lpt.security.AccountPrincipal;
import com.example.lpt.security.LoginRateLimiter;
import com.example.lpt.security.SessionManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Authentication dependencies (encoder, repository, session manager) are mocked. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-01T09:00:00Z");

    @Mock AccountRepository accounts;
    @Mock PasswordEncoder encoder;
    @Mock SessionManager sessions;

    final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    LoginRateLimiter limiter;
    AuthService service;

    @BeforeEach
    void setUp() {
        limiter = new LoginRateLimiter(5, 15, clock);
        service = new AuthService(accounts, encoder, limiter, sessions, clock);
    }

    private static Account candidate(boolean mustChange, Instant expiresAt) {
        Account account = new Account("a@b.test", "stored-hash", Role.CANDIDATE, mustChange, expiresAt);
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }

    private static void assertApiError(Throwable thrown, HttpStatus status, String message) {
        assertThat(thrown).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            assertThat(e.getMessage()).isEqualTo(message);
        });
    }

    // ------------------------------------------------------------------ login

    @Test
    void loginSucceedsAndStartsASession() {
        Account account = candidate(true, NOW.plusSeconds(3600));
        when(accounts.findByEmail("a@b.test")).thenReturn(Optional.of(account));
        when(encoder.matches("pw", "stored-hash")).thenReturn(true);

        LoginResponse result = service.login(new LoginRequest("  A@B.test ", "pw"), request, response);

        assertThat(result.role()).isEqualTo("CANDIDATE");
        assertThat(result.mustChangePassword()).isTrue();
        verify(sessions).establish(account, request, response);
    }

    @Test
    void unknownEmailGetsTheGenericErrorAndStillChecksAHash() {
        when(accounts.findByEmail("nobody@b.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@b.test", "pw"), request, response))
                .satisfies(e -> assertApiError(e, HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        verify(encoder).matches(eq("pw"), any()); // work is done either way, so timing does not reveal the account
        verify(sessions, never()).establish(any(), any(), any());
    }

    @Test
    void wrongPasswordGetsTheGenericError() {
        when(accounts.findByEmail("a@b.test")).thenReturn(Optional.of(candidate(false, null)));
        when(encoder.matches("bad", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("a@b.test", "bad"), request, response))
                .satisfies(e -> assertApiError(e, HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        verify(sessions, never()).establish(any(), any(), any());
    }

    @Test
    void expiredTemporaryPasswordGetsTheSameGenericError() {
        when(accounts.findByEmail("a@b.test")).thenReturn(Optional.of(candidate(true, NOW.minusSeconds(1))));
        when(encoder.matches("pw", "stored-hash")).thenReturn(true); // password is right, but it has expired

        assertThatThrownBy(() -> service.login(new LoginRequest("a@b.test", "pw"), request, response))
                .satisfies(e -> assertApiError(e, HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        verify(sessions, never()).establish(any(), any(), any());
    }

    @Test
    void blockedEmailIsRefusedBeforeAnyLookup() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("email:a@b.test");
        }

        assertThatThrownBy(() -> service.login(new LoginRequest("a@b.test", "pw"), request, response))
                .satisfies(e -> assertApiError(e, HttpStatus.TOO_MANY_REQUESTS,
                        "Too many sign-in attempts. Please try again later."));
        verifyNoInteractions(accounts);
    }

    @Test
    void failedLoginsCountTowardTheLimitAndSuccessResetsIt() {
        Account account = candidate(false, null);
        when(accounts.findByEmail("a@b.test")).thenReturn(Optional.of(account));
        when(encoder.matches("bad", "stored-hash")).thenReturn(false);
        when(encoder.matches("good", "stored-hash")).thenReturn(true);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.login(new LoginRequest("a@b.test", "bad"), request, response))
                    .isInstanceOf(ApiException.class);
        }
        service.login(new LoginRequest("a@b.test", "good"), request, response); // 5th attempt is fine and resets

        assertThat(limiter.isBlocked("email:a@b.test", 5)).isFalse();
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.login(new LoginRequest("a@b.test", "bad"), request, response))
                    .isInstanceOf(ApiException.class);
        }
        assertThat(limiter.isBlocked("email:a@b.test", 5)).isFalse(); // 4 new failures: still allowed
    }

    // ------------------------------------------------------------------ change password

    private static AccountPrincipal principal() {
        return new AccountPrincipal(1L, "a@b.test", Role.CANDIDATE, true);
    }

    @Test
    void changePasswordStoresNewHashClearsTemporaryFlagsAndRotatesSession() {
        Account account = candidate(true, NOW.plusSeconds(3600));
        when(accounts.findById(1L)).thenReturn(Optional.of(account));
        when(encoder.matches("temp-password", "stored-hash")).thenReturn(true);
        when(encoder.encode("New-password-1")).thenReturn("new-hash");

        service.changePassword(principal(), new ChangePasswordRequest("temp-password", "New-password-1"), request, response);

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("new-hash");
        assertThat(saved.getValue().isMustChangePassword()).isFalse();
        assertThat(saved.getValue().getTempPasswordExpiresAt()).isNull();
        verify(sessions).expireAllFor(1L);
        verify(sessions).establish(account, request, response);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        when(accounts.findById(1L)).thenReturn(Optional.of(candidate(true, NOW.plusSeconds(3600))));
        when(encoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(principal(),
                new ChangePasswordRequest("wrong", "New-password-1"), request, response))
                .satisfies(e -> assertApiError(e, HttpStatus.BAD_REQUEST, "Current password is incorrect."));
        verify(accounts, never()).save(any());
    }

    @Test
    void changePasswordRejectsExpiredTemporaryPasswordAndEndsTheSession() {
        when(accounts.findById(1L)).thenReturn(Optional.of(candidate(true, NOW.minusSeconds(5))));

        assertThatThrownBy(() -> service.changePassword(principal(),
                new ChangePasswordRequest("temp-password", "New-password-1"), request, response))
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(sessions).logout(request, response);
        verify(accounts, never()).save(any());
    }

    @Test
    void changePasswordRejectsReusingTheCurrentPassword() {
        when(accounts.findById(1L)).thenReturn(Optional.of(candidate(true, NOW.plusSeconds(3600))));
        when(encoder.matches("same-password", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(principal(),
                new ChangePasswordRequest("same-password", "same-password"), request, response))
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(accounts, never()).save(any());
    }
}
