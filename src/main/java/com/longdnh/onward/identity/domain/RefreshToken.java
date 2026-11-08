package com.longdnh.donot_get_left_behind.identity.domain;

import static com.longdnh.donot_get_left_behind.common.Validation.requireUuidV7;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.Getter;

@Getter
public final class RefreshToken {

    public static final int TOKEN_HASH_LENGTH = 64;
    private static final Pattern TOKEN_HASH_PATTERN = Pattern.compile("[0-9a-f]{" + TOKEN_HASH_LENGTH + "}");

    private final UUID id;
    private final UUID sessionId;
    private final String tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant consumedAt;
    private final UUID replacedByTokenId;

    /** Reconstitutes state using a SHA-256 hash in lowercase hexadecimal, never the raw credential. */
    public RefreshToken(UUID id, UUID sessionId, String tokenHash, Instant createdAt, Instant expiresAt,
                        Instant consumedAt, UUID replacedByTokenId) {
        this.id = requireUuidV7(id, "id");
        this.sessionId = requireUuidV7(sessionId, "sessionId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash is required");
        if (!TOKEN_HASH_PATTERN.matcher(tokenHash).matches()) {
            throw new IllegalArgumentException("tokenHash must be a SHA-256 hash encoded as 64 lowercase hexadecimal characters");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        this.consumedAt = consumedAt;
        this.replacedByTokenId = replacedByTokenId == null ? null
                : requireUuidV7(replacedByTokenId, "replacedByTokenId");

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
        if ((consumedAt == null) != (replacedByTokenId == null)) {
            throw new IllegalArgumentException("consumedAt and replacedByTokenId must both be present or both be absent");
        }
        if (consumedAt != null && (consumedAt.isBefore(createdAt) || !consumedAt.isBefore(expiresAt))) {
            throw new IllegalArgumentException("consumedAt must be on or after createdAt and before expiresAt");
        }
        if (id.equals(replacedByTokenId)) {
            throw new IllegalArgumentException("A refresh token cannot replace itself");
        }
    }

    public static RefreshToken create(UUID id, UUID sessionId, String tokenHash, Instant now, Instant expiresAt) {
        return new RefreshToken(id, sessionId, tokenHash, now, expiresAt, null, null);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    /** Checks local token state only; the application must also validate its session and user. */
    public boolean isUsableAt(Instant now) {
        Objects.requireNonNull(now, "now is required");
        return !isConsumed() && !now.isBefore(createdAt) && now.isBefore(expiresAt);
    }

    /** The application must persist this transition and its replacement in one transaction. */
    public RefreshToken consume(UUID replacementTokenId, Instant now) {
        UUID replacementId = requireUuidV7(replacementTokenId, "replacementTokenId");
        Objects.requireNonNull(now, "now is required");
        if (id.equals(replacementId)) {
            throw new IllegalArgumentException("A refresh token cannot replace itself");
        }
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("now must not precede createdAt");
        }
        if (isConsumed()) {
            throw new IllegalStateException("Refresh token has already been consumed");
        }
        if (!isUsableAt(now)) {
            throw new IllegalStateException("Cannot consume an expired refresh token");
        }
        return new RefreshToken(id, sessionId, tokenHash, createdAt, expiresAt, now, replacementId);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof RefreshToken token && id.equals(token.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "RefreshToken{id=" + id + ", sessionId=" + sessionId + ", consumed=" + isConsumed() + "}";
    }
}
