package com.example.lpt.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security 6 loads the CSRF token lazily. Touching it here makes sure the
 * XSRF-TOKEN cookie is sent on every response so the React client can echo it back
 * in the X-XSRF-TOKEN header.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
        if (token != null) {
            token.getToken();
        }
        chain.doFilter(request, response);
    }
}
