package com.securetravels.crm.common.config;

import com.securetravels.crm.user.Role;
import com.securetravels.crm.user.User;
import com.securetravels.crm.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures a dedicated Finance ERP login exists for Trip On Himalaya.
 * Idempotent: creates {@code finance@securetravels.in} only when missing.
 * Override password with env {@code FINANCE_BOOTSTRAP_PASSWORD}.
 */
@Component
@Order(20)
public class BootstrapFinanceUserRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapFinanceUserRunner.class);
    private static final String EMAIL = "finance@securetravels.in";
    private static final String DEFAULT_PASSWORD = "finance123";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public BootstrapFinanceUserRunner(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.existsByEmailIgnoreCase(EMAIL)) {
            return;
        }
        String password = System.getenv("FINANCE_BOOTSTRAP_PASSWORD");
        if (password == null || password.isBlank()) {
            password = DEFAULT_PASSWORD;
        }
        User finance = new User(
                EMAIL,
                passwordEncoder.encode(password),
                "Finance Officer",
                Role.FINANCE,
                "9876000006");
        users.save(finance);
        log.info("[bootstrap-finance] created dedicated FINANCE login {}", EMAIL);
    }
}
