package com.longdnh.onward.identity.infrastructure.bootstrap;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.longdnh.onward.identity.application.port.UserRepository;
import com.longdnh.onward.identity.application.service.AccountService;

import lombok.extern.slf4j.Slf4j;

/**
 * Grants the first system admin from configuration. Runs on every start and is idempotent:
 * it does nothing once any admin exists. The user must already be registered; no password is
 * ever seeded. Later admins are promoted through the admin API.
 */
@Slf4j
@Component
public class SystemAdminBootstrap implements ApplicationRunner {

    private final String adminEmail;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final TransactionTemplate transactionTemplate;

    public SystemAdminBootstrap(
            @Value("${app.bootstrap.system-admin-email:}") String adminEmail,
            UserRepository userRepository,
            AccountService accountService,
            TransactionTemplate transactionTemplate) {
        this.adminEmail = adminEmail == null ? "" : adminEmail.strip().toLowerCase(Locale.ROOT);
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isEmpty()) {
            log.info("System admin bootstrap skipped: app.bootstrap.system-admin-email is not set");
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            if (userRepository.existsSystemAdmin()) {
                log.info("System admin bootstrap skipped: an admin already exists");
                return;
            }
            userRepository.findByEmail(adminEmail).ifPresentOrElse(
                    user -> {
                        accountService.promoteToSystemAdmin(user.getId());
                        log.warn("System admin bootstrap: promoted user {}", user.getId());
                    },
                    () -> log.warn("System admin bootstrap: no registered user for the configured email; register it, then restart"));
        });
    }
}
