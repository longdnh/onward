package com.longdnh.onward.identity.application.event;

import java.time.Instant;

/**
 * Published inside the transaction that stores the token hash. The mail adapter listens AFTER_COMMIT, so
 * a rolled-back registration never produces a link that points at nothing. The raw token travels only
 * in memory here; it is never logged or persisted.
 */
public record EmailVerificationRequested(String email, String rawToken, Instant expiresAt) {

    @Override
    public String toString() {
        return "EmailVerificationRequested[email=" + email + "]";
    }
}
