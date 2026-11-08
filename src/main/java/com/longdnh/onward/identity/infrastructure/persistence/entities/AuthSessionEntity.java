package com.longdnh.onward.identity.infrastructure.persistence.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.longdnh.onward.identity.domain.AuthSession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "auth_sessions")
public class AuthSessionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "absolute_expires_at", nullable = false, updatable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AuthSessionEntity() {
    }

    public static AuthSessionEntity fromDomain(AuthSession session) {
        Objects.requireNonNull(session, "session is required");

        AuthSessionEntity entity = new AuthSessionEntity();
        entity.id = session.getId();
        entity.userId = session.getUserId();
        entity.createdAt = session.getCreatedAt();
        entity.lastSeenAt = session.getLastSeenAt();
        entity.absoluteExpiresAt = session.getAbsoluteExpiresAt();
        entity.revokedAt = session.getRevokedAt();

        return entity;
    }

    public AuthSession toDomain() {
        return new AuthSession(
                id,
                userId,
                createdAt,
                lastSeenAt,
                absoluteExpiresAt,
                revokedAt);
    }

    public void revoke(Instant now) {
        AuthSession revoked = this.toDomain().revoke(now);

        this.revokedAt = revoked.getRevokedAt();
    }
}