package com.longdnh.onward.identity.application.event;

import java.time.Instant;

/** See EmailVerificationRequested for the after-commit contract. */
public record PasswordResetRequested(String email, String rawToken, Instant expiresAt) {

    @Override
    public String toString() {
        return "PasswordResetRequested[email=" + email + "]";
    }
}
