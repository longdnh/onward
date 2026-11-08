package com.longdnh.onward.identity.infrastructure.persistence.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.longdnh.onward.identity.domain.RefreshToken;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(name = "refresh_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uq_refresh_tokens_hash", columnNames = "token_hash"),
        @UniqueConstraint(name = "uq_refresh_tokens_session_id", columnNames = { "session_id", "id" })
})
public class RefreshTokenEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = RefreshToken.TOKEN_HASH_LENGTH)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    protected RefreshTokenEntity() {
    }

    public static RefreshTokenEntity fromDomain(RefreshToken token) {
        Objects.requireNonNull(token, "token is required");

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.id = token.getId();
        entity.sessionId = token.getSessionId();
        entity.tokenHash = token.getTokenHash();
        entity.createdAt = token.getCreatedAt();
        entity.expiresAt = token.getExpiresAt();
        entity.consumedAt = token.getConsumedAt();
        entity.replacedByTokenId = token.getReplacedByTokenId();

        return entity;
    }

    public RefreshToken toDomain() {
        return new RefreshToken(
                id,
                sessionId,
                tokenHash,
                createdAt,
                expiresAt,
                consumedAt,
                replacedByTokenId);
    }

    public void consume(UUID replacementTokenId, Instant now) {
        RefreshToken consumed = this.toDomain().consume(replacementTokenId, now);

        this.consumedAt = consumed.getConsumedAt();
        this.replacedByTokenId = consumed.getReplacedByTokenId();
    }
}