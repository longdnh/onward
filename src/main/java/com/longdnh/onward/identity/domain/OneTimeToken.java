package com.longdnh.onward.identity.domain;

import static com.longdnh.onward.common.Validation.requireUuidV7;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.Getter;

/**
 * Persisted state of a single-use secret sent by email (verification link, password-reset link).
 * Stores only the SHA-256 hash of the raw secret; infrastructure generates and hashes the secret.
 * Unlike RefreshToken, consumption has no replacement: the token is simply spent.
 */
@Getter
public final class OneTimeToken {

    public static final int TOKEN_HASH_LENGTH = 64;
    private static final Pattern TOKEN_HASH_PATTERN = Pattern.compile("[0-9a-f]{" + TOKEN_HASH_LENGTH + "}");

    private final UUID id;
    private final UUID userId;
    private final OneTimeTokenPurpose purpose;
    private final String tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant consumedAt;

    public OneTimeToken(UUID id, UUID userId, OneTimeTokenPurpose purpose, String tokenHash, Instant createdAt,
                        Instant expiresAt, Instant consumedAt) {
        this.id = requireUuidV7(id, "id");
        this.userId = requireUuidV7(userId, "userId");
        this.purpose = Objects.requireNonNull(purpose, "purpose is required");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash is required");
        if (!TOKEN_HASH_PATTERN.matcher(tokenHash).matches()) {
            throw new IllegalArgumentException("tokenHash must be a SHA-256 hash encoded as 64 lowercase hexadecimal characters");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        this.consumedAt = consumedAt;

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
        if (consumedAt != null && consumedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("consumedAt must not precede createdAt");
        }
    }

    public static OneTimeToken create(UUID id, UUID userId, OneTimeTokenPurpose purpose, String tokenHash,
                                      Instant now, Instant expiresAt) {
        return new OneTimeToken(id, userId, purpose, tokenHash, now, expiresAt, null);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    /** Local token state only; the application must also check the purpose and the user. */
    public boolean isUsableAt(Instant now) {
        Objects.requireNonNull(now, "now is required");
        return !isConsumed() && !now.isBefore(createdAt) && now.isBefore(expiresAt);
    }

    /** Spends the token. Expired tokens may be consumed too (used to invalidate superseded tokens). */
    public OneTimeToken consume(Instant now) {
        Objects.requireNonNull(now, "now is required");
        if (isConsumed()) {
            return this;
        }
        return new OneTimeToken(id, userId, purpose, tokenHash, createdAt, expiresAt, now);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof OneTimeToken token && id.equals(token.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "OneTimeToken{id=" + id + ", userId=" + userId + ", purpose=" + purpose + ", consumed=" + isConsumed() + "}";
    }
}
