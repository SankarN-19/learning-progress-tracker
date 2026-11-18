package com.example.lpt.config;

import com.example.lpt.security.CsrfCookieFilter;
import com.example.lpt.security.JsonErrorWriter;
import com.example.lpt.security.SessionGuardFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SessionRegistry sessionRegistry,
                                            SecurityContextRepository securityContextRepository,
                                            JsonErrorWriter errors,
                                            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies)
            throws Exception {

        // CSRF token lives in a JS-readable cookie (XSRF-TOKEN); the client echoes it in X-XSRF-TOKEN.
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(c -> c.secure(secureCookies).sameSite("Lax"));

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)   // POST /api/v1/auth/logout is a normal controller
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .requestMatchers("/api/v1/me/**").hasRole("CANDIDATE")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/assignments/*/status").hasRole("CANDIDATE")
                        .requestMatchers("/api/v1/**").hasRole("COORDINATOR")
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, ex) ->
                                errors.write(res, 401, "Authentication required."))
                        .accessDeniedHandler((req, res, ex) ->
                                errors.write(res, 403, "You do not have permission to perform this action.")))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(new SessionGuardFilter(sessionRegistry, errors), AnonymousAuthenticationFilter.class);

        return http.build();
    }
}
