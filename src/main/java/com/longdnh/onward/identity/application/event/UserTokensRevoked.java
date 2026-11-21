package com.longdnh.onward.identity.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published inside the transaction that revokes every session of a user (password reset, account disable).
 * Carries the instant so the gateway can reject only tokens issued before it: a session opened after a
 * password reset must work immediately, and its tokens have a later {@code iat}.
 */
public record UserTokensRevoked(UUID userId, Instant revokedAt) {
}
