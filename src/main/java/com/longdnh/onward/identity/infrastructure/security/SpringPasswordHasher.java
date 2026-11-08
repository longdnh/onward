package com.longdnh.donot_get_left_behind.identity.infrastructure.security;

import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.longdnh.donot_get_left_behind.identity.application.port.PasswordHasher;

@Component
public final class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public SpringPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(Objects.requireNonNull(rawPassword, "rawPassword is required"));
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(Objects.requireNonNull(rawPassword, "rawPassword is required"),
                Objects.requireNonNull(encodedPassword, "encodedPassword is required"));
    }
}
