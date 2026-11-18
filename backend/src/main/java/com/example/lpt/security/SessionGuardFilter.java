package com.example.lpt.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Two server-side rules that run on every authenticated request:
 * 1. A session the coordinator expired (password reset) or that we expired is rejected with 401.
 * 2. A session whose account still has a temporary password may only call
 *    change-password and logout (plus login / me, which expose no training data) - otherwise 403.
 */
public class SessionGuardFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_WHEN_PASSWORD_CHANGE_PENDING = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/change-password",
            "/api/v1/auth/logout",
            "/api/v1/auth/me");

    private final SessionRegistry sessionRegistry;
    private final JsonErrorWriter errors;

    public SessionGuardFilter(SessionRegistry sessionRegistry, JsonErrorWriter errors) {
        this.sessionRegistry = sessionRegistry;
        this.errors = errors;
    }

    /** Request path without the context path (getServletPath() is empty under MockMvc, so it is not used). */
    private static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context != null && !context.isEmpty() && uri.startsWith(context) ? uri.substring(context.length()) : uri;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AccountPrincipal principal) {
            HttpSession session = request.getSession(false);
            SessionInformation info = session == null ? null : sessionRegistry.getSessionInformation(session.getId());
            if (info != null && info.isExpired()) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                errors.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Your session has ended. Please sign in again.");
                return;
            }
            if (principal.mustChangePassword()
                    && !ALLOWED_WHEN_PASSWORD_CHANGE_PENDING.contains(pathOf(request))) {
                errors.write(response, HttpServletResponse.SC_FORBIDDEN,
                        "You must change your temporary password before continuing.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}