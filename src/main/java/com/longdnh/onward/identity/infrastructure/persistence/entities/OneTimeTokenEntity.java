package com.longdnh.onward.identity.infrastructure.persistence.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.longdnh.onward.identity.domain.OneTimeToken;
import com.longdnh.onward.identity.domain.OneTimeTokenPurpose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(name = "one_time_tokens", uniqueConstraints = @UniqueConstraint(name = "uq_one_time_tokens_hash", columnNames = "token_hash"))
public class OneTimeTokenEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, updatable = false, length = 32)
    private OneTimeTokenPurpose purpose;

    @Column(name = "token_hash", nullable = false, updatable = false, length = OneTimeToken.TOKEN_HASH_LENGTH)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected OneTimeTokenEntity() {
    }

    public static OneTimeTokenEntity fromDomain(OneTimeToken token) {
        Objects.requireNonNull(token, "token is required");
        OneTimeTokenEntity entity = new OneTimeTokenEntity();
        entity.id = token.getId();
        entity.userId = token.getUserId();
        entity.purpose = token.getPurpose();
        entity.tokenHash = token.getTokenHash();
        entity.createdAt = token.getCreatedAt();
        entity.expiresAt = token.getExpiresAt();
        entity.consumedAt = token.getConsumedAt();
        return entity;
    }

    public OneTimeToken toDomain() {
        return new OneTimeToken(id, userId, purpose, tokenHash, createdAt, expiresAt, consumedAt);
    }
}
