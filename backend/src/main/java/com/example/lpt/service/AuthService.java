package com.example.lpt.service;

import com.example.lpt.dto.ChangePasswordRequest;
import com.example.lpt.dto.LoginRequest;
import com.example.lpt.dto.LoginResponse;
import com.example.lpt.entity.Account;
import com.example.lpt.exception.ApiException;
import com.example.lpt.repository.AccountRepository;
import com.example.lpt.security.AccountPrincipal;
import com.example.lpt.security.LoginRateLimiter;
import com.example.lpt.security.SessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String GENERIC_LOGIN_ERROR = "Invalid email or password.";
    private static final int IP_LIMIT_FACTOR = 4; // an IP may fail more often than one email (shared networks)

    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final LoginRateLimiter rateLimiter;
    private final SessionManager sessions;
    private final Clock clock;
    /** Compared against when the email is unknown, so timing does not reveal whether it exists. */
    private final String dummyHash;

    public AuthService(AccountRepository accounts, PasswordEncoder encoder, LoginRateLimiter rateLimiter,
                       SessionManager sessions, Clock clock) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.rateLimiter = rateLimiter;
        this.sessions = sessions;
        this.clock = clock;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        String email = normalize(request.email());
        String emailKey = "email:" + email;
        String ipKey = "ip:" + http.getRemoteAddr();

        if (rateLimiter.isBlocked(emailKey, rateLimiter.maxAttempts())
                || rateLimiter.isBlocked(ipKey, rateLimiter.maxAttempts() * IP_LIMIT_FACTOR)) {
            throw ApiException.tooManyRequests("Too many sign-in attempts. Please try again later.");
        }

        Account account = accounts.findByEmail(email).orElse(null);
        String hash = account != null ? account.getPasswordHash() : dummyHash;
        boolean passwordMatches = encoder.matches(request.password(), hash);

        // Unknown email, wrong password and expired temporary password all look identical to the caller.
        if (account == null || !passwordMatches || account.isTemporaryPasswordExpired(clock.instant())) {
            rateLimiter.recordFailure(emailKey);
            rateLimiter.recordFailure(ipKey);
            throw ApiException.unauthorized(GENERIC_LOGIN_ERROR);
        }

        rateLimiter.reset(emailKey);
        sessions.establish(account, http, response);
        return new LoginResponse(account.getRole().name(), account.isMustChangePassword());
    }

    public void changePassword(AccountPrincipal principal, ChangePasswordRequest request,
                               HttpServletRequest http, HttpServletResponse response) {
        Account account = accounts.findById(principal.accountId())
                .orElseThrow(() -> ApiException.unauthorized("Authentication required."));

        if (account.isTemporaryPasswordExpired(clock.instant())) {
            sessions.logout(http, response);
            throw ApiException.unauthorized("Your temporary password has expired. Ask a coordinator for a reset.");
        }
        if (!encoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw ApiException.badRequest("currentPassword", "Current password is incorrect.");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw ApiException.badRequest("newPassword", "New password must be different from the current one.");
        }

        account.setPasswordHash(encoder.encode(request.newPassword()));
        account.setMustChangePassword(false);
        account.setTempPasswordExpiresAt(null);
        accounts.save(account);

        // Every other session of this account ends; the caller gets a fresh, fully-privileged session.
        sessions.expireAllFor(account.getId());
        sessions.establish(account, http, response);
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
