package com.example.lpt.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email; // always stored lower-cased

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "temp_password_expires_at")
    private Instant tempPasswordExpiresAt;

    protected Account() {
    }

    public Account(String email, String passwordHash, Role role, boolean mustChangePassword,
                   Instant tempPasswordExpiresAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
        this.tempPasswordExpiresAt = tempPasswordExpiresAt;
    }

    /** True when this account still has a temporary password and its 24h window has passed. */
    public boolean isTemporaryPasswordExpired(Instant now) {
        return mustChangePassword && tempPasswordExpiresAt != null && !now.isBefore(tempPasswordExpiresAt);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public Instant getTempPasswordExpiresAt() { return tempPasswordExpiresAt; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public void setTempPasswordExpiresAt(Instant tempPasswordExpiresAt) { this.tempPasswordExpiresAt = tempPasswordExpiresAt; }
}
