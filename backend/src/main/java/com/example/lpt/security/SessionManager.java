package com.example.lpt.security;

import com.example.lpt.entity.Account;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Creates, rotates and ends server-side sessions (cookie = opaque session id only). */
@Component
public class SessionManager {

    private static final String SESSION_COOKIE = "JSESSIONID";

    private final SessionRegistry sessionRegistry;
    private final SecurityContextRepository securityContextRepository;

    public SessionManager(SessionRegistry sessionRegistry, SecurityContextRepository securityContextRepository) {
        this.sessionRegistry = sessionRegistry;
        this.securityContextRepository = securityContextRepository;
    }

    /** Starts a brand-new session for the account (any old session is destroyed: no fixation). */
    public void establish(Account account, HttpServletRequest request, HttpServletResponse response) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        AccountPrincipal principal = AccountPrincipal.from(account);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.authorities()));
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        securityContextRepository.saveContext(context, request, response);
        sessionRegistry.registerNewSession(session.getId(), principal);
    }

    /** Marks every session of the account as expired; SessionGuardFilter rejects them with 401. */
    public void expireAllFor(Long accountId) {
        for (Object p : sessionRegistry.getAllPrincipals()) {
            if (p instanceof AccountPrincipal ap && ap.accountId().equals(accountId)) {
                sessionRegistry.getAllSessions(p, false).forEach(info -> info.expireNow());
            }
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        ResponseCookie cleared = ResponseCookie.from(SESSION_COOKIE, "")
                .path("/").httpOnly(true).maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
    }
}
