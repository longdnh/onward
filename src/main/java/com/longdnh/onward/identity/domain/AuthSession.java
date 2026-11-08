package com.longdnh.donot_get_left_behind.identity.domain;

import static com.longdnh.donot_get_left_behind.common.Validation.requireUuidV7;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

@Getter
public final class AuthSession {

    private final UUID id;
    private final UUID userId;
    private final Instant createdAt;
    private final Instant lastSeenAt;
    private final Instant absoluteExpiresAt;
    private final Instant revokedAt;

    /** Reconstitutes persisted state; revokedAt is null until the session is revoked. */
    public AuthSession(UUID id, UUID userId, Instant createdAt, Instant lastSeenAt, Instant absoluteExpiresAt,
                       Instant revokedAt) {
        this.id = requireUuidV7(id, "id");
        this.userId = requireUuidV7(userId, "userId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.lastSeenAt = Objects.requireNonNull(lastSeenAt, "lastSeenAt is required");
        this.absoluteExpiresAt = Objects.requireNonNull(absoluteExpiresAt, "absoluteExpiresAt is required");
        this.revokedAt = revokedAt;

        if (!absoluteExpiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("absoluteExpiresAt must be after createdAt");
        }
        if (lastSeenAt.isBefore(createdAt) || !lastSeenAt.isBefore(absoluteExpiresAt)) {
            throw new IllegalArgumentException("lastSeenAt must be on or after createdAt and before absoluteExpiresAt");
        }
        if (revokedAt != null && revokedAt.isBefore(lastSeenAt)) {
            throw new IllegalArgumentException("revokedAt must not precede lastSeenAt");
        }
    }

    public static AuthSession create(UUID id, UUID userId, Instant now, Instant absoluteExpiresAt) {
        return new AuthSession(id, userId, now, now, absoluteExpiresAt, null);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** Checks this session's lifetime only; the application must also check the user. */
    public boolean isActiveAt(Instant now, Duration idleTimeout) {
        Objects.requireNonNull(now, "now is required");
        Objects.requireNonNull(idleTimeout, "idleTimeout is required");
        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("idleTimeout must be positive");
        }

        return !isRevoked()
                && !now.isBefore(lastSeenAt)
                && now.isBefore(absoluteExpiresAt)
                && Duration.between(lastSeenAt, now).compareTo(idleTimeout) < 0;
    }

    /** Returns new state without extending the absolute lifetime or reviving an expired session. */
    public AuthSession recordActivity(Instant now, Duration idleTimeout) {
        requireChangeTime(now);
        if (!isActiveAt(now, idleTimeout)) {
            throw new IllegalStateException("Cannot record activity for an inactive session");
        }
        if (now.equals(lastSeenAt)) {
            return this;
        }
        return new AuthSession(id, userId, createdAt, now, absoluteExpiresAt, null);
    }

    /** Revocation is allowed after expiration; repeated calls preserve the first revocation time. */
    public AuthSession revoke(Instant now) {
        requireChangeTime(now);
        if (isRevoked()) {
            return this;
        }
        return new AuthSession(id, userId, createdAt, lastSeenAt, absoluteExpiresAt, now);
    }

    private void requireChangeTime(Instant now) {
        Objects.requireNonNull(now, "now is required");
        if (now.isBefore(lastSeenAt)) {
            throw new IllegalArgumentException("now must not precede lastSeenAt");
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AuthSession session && id.equals(session.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "AuthSession{id=" + id + ", userId=" + userId + ", revoked=" + isRevoked() + "}";
    }
}
