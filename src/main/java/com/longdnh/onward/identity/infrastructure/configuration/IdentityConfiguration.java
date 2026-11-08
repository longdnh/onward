package com.longdnh.donot_get_left_behind.identity.infrastructure.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.longdnh.donot_get_left_behind.identity.application.RegistrationPolicy;
import com.longdnh.donot_get_left_behind.identity.application.port.AccessTokenIssuer;
import com.longdnh.donot_get_left_behind.identity.application.port.AuthSessionRepository;
import com.longdnh.donot_get_left_behind.common.id.IdGenerator;
import com.longdnh.donot_get_left_behind.identity.application.port.PasswordHasher;
import com.longdnh.donot_get_left_behind.identity.application.port.RefreshTokenGenerator;
import com.longdnh.donot_get_left_behind.identity.application.port.RefreshTokenRepository;
import com.longdnh.donot_get_left_behind.identity.application.port.UserRepository;
import com.longdnh.donot_get_left_behind.identity.application.service.LoginService;
import com.longdnh.donot_get_left_behind.identity.application.service.LogoutService;
import com.longdnh.donot_get_left_behind.identity.application.service.RefreshService;

import jakarta.validation.Validator;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
public class IdentityConfiguration {

    private final AuthProperties authProperties;

    IdentityConfiguration(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Salt/hash sizes are bytes; memory is KiB: 19 MiB, two iterations, parallelism
        // one.
        PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
        return new DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2));
    }

    @Bean
    public RegistrationPolicy registrationPolicy(Validator validator) throws IOException {
        ClassPathResource resource = new ClassPathResource("security/common-passwords.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            Set<String> blockedPasswords = reader.lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .collect(Collectors.toUnmodifiableSet());
            return new RegistrationPolicy(validator, blockedPasswords);
        }
    }

    @Bean
    public LoginService loginService(
            UserRepository userRepository,
            AuthSessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenIssuer accessTokenIssuer,
            IdGenerator idGenerator,
            Clock clock,
            AuthProperties properties,
            PlatformTransactionManager transactionManager) {

        String dummyPasswordHash = passwordHasher.hash(refreshTokenGenerator.generate());

        return new LoginService(
                userRepository,
                sessionRepository,
                refreshTokenRepository,
                passwordHasher,
                refreshTokenGenerator,
                accessTokenIssuer,
                idGenerator,
                clock,
                properties.accessTokenLifetime(),
                properties.absoluteLifetime(),
                dummyPasswordHash,
                new TransactionTemplate(transactionManager));
    }

    @Bean
    public RefreshService refreshService(
            UserRepository userRepository,
            AuthSessionRepository authSessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenIssuer accessTokenIssuer,
            IdGenerator idGenerator,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        return new RefreshService(
                userRepository,
                authSessionRepository,
                refreshTokenRepository,
                refreshTokenGenerator,
                accessTokenIssuer,
                idGenerator,
                clock,
                authProperties.accessTokenLifetime(),
                authProperties.idleTimeout(),
                new TransactionTemplate(transactionManager));
    }

    @Bean
    public LogoutService logoutService(
            RefreshTokenRepository refreshTokenRepository,
            AuthSessionRepository authSessionRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        return new LogoutService(refreshTokenRepository, authSessionRepository, refreshTokenGenerator,
                new TransactionTemplate(transactionManager), clock);
    }
}
