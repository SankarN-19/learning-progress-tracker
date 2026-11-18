package com.example.lpt.controller;

import com.example.lpt.dto.ChangePasswordRequest;
import com.example.lpt.dto.LoginRequest;
import com.example.lpt.dto.LoginResponse;
import com.example.lpt.dto.MeResponse;
import com.example.lpt.security.AccountPrincipal;
import com.example.lpt.security.SessionManager;
import com.example.lpt.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionManager sessions;

    public AuthController(AuthService authService, SessionManager sessions) {
        this.authService = authService;
        this.sessions = sessions;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest http, HttpServletResponse response) {
        return authService.login(request, http, response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AccountPrincipal principal,
                                               @Valid @RequestBody ChangePasswordRequest request,
                                               HttpServletRequest http, HttpServletResponse response) {
        authService.changePassword(principal, request, http, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http, HttpServletResponse response) {
        sessions.logout(http, response);
        return ResponseEntity.noContent().build();
    }

    /** Small addition to the brief's contract: lets the React route guards restore state after a refresh. */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
        return new MeResponse(principal.email(), principal.role().name(), principal.mustChangePassword());
    }
}
