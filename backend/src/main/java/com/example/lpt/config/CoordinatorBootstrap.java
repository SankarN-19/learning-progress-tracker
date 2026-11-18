package com.example.lpt.config;

import com.example.lpt.entity.Account;
import com.example.lpt.entity.Role;
import com.example.lpt.repository.AccountRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the initial coordinator from APP_COORDINATOR_EMAIL / APP_COORDINATOR_PASSWORD on first run.
 * No hard-coded credentials and no public registration. The password is never logged.
 */
@Component
public class CoordinatorBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CoordinatorBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public CoordinatorBootstrap(AccountRepository accounts, PasswordEncoder encoder,
                                @Value("${app.coordinator.email:}") String email,
                                @Value("${app.coordinator.password:}") String password) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.existsByRole(Role.COORDINATOR)) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("No coordinator account exists. Set APP_COORDINATOR_EMAIL and "
                    + "APP_COORDINATOR_PASSWORD and restart to create one.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "APP_COORDINATOR_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        accounts.save(new Account(normalized, encoder.encode(password), Role.COORDINATOR, false, null));
        log.info("Created initial coordinator account for {}", normalized);
    }
}
