package com.example.lpt.dto;

import java.time.Instant;

/** One-time credentials. Returned once, never stored in plain text, never logged. */
public record CredentialsResponse(String username, String temporaryPassword, Instant expiresAt) {
}
