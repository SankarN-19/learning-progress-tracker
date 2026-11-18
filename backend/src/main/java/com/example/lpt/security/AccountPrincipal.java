package com.example.lpt.security;

import com.example.lpt.entity.Account;
import com.example.lpt.entity.Role;
import java.io.Serializable;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * What we keep in the server-side session. Deliberately has NO password hash.
 * Identity is the account id; equality ignores the other fields so the session
 * registry can find every session of one account.
 */
public record AccountPrincipal(Long accountId, String email, Role role, boolean mustChangePassword)
        implements Serializable {

    public static AccountPrincipal from(Account a) {
        return new AccountPrincipal(a.getId(), a.getEmail(), a.getRole(), a.isMustChangePassword());
    }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AccountPrincipal other && accountId.equals(other.accountId);
    }

    @Override
    public int hashCode() {
        return accountId.hashCode();
    }
}
