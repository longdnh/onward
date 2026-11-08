package com.longdnh.onward.identity.infrastructure.persistence.adapter;

import static com.longdnh.onward.common.Validation.requireUuidV7;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.onward.identity.application.port.AuthSessionRepository;
import com.longdnh.onward.identity.domain.AuthSession;
import com.longdnh.onward.identity.infrastructure.persistence.entities.AuthSessionEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Repository
@Transactional(readOnly = true)
public class AuthSessionRepositoryAdapter implements AuthSessionRepository {

    private final EntityManager entityManager;

    public AuthSessionRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public AuthSession create(AuthSession session) {
        AuthSessionEntity entity = AuthSessionEntity.fromDomain(session);

        entityManager.persist(entity);

        return entity.toDomain();
    }

    @Override
    public Optional<AuthSession> findById(UUID id) {
        UUID sessionId = requireUuidV7(id, "id");

        AuthSessionEntity entity = entityManager.find(
                AuthSessionEntity.class,
                sessionId);

        return Optional.ofNullable(entity)
                .map(AuthSessionEntity::toDomain);
    }

    @Override
    @Transactional
    public boolean recordActivityIfActive(UUID sessionId, UUID userId, Instant now, Duration idleTimeout) {
        requireUuidV7(sessionId, "sessionId");
        requireUuidV7(userId, "userId");

        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "idleTimeout must be positive");
        }

        int updated = entityManager.createQuery("""
                update AuthSessionEntity s
                   set s.lastSeenAt = :now
                   where s.id = :sessionId
                   and s.userId = :userId
                   and s.revokedAt is null
                   and s.absoluteExpiresAt > :now
                   and s.lastSeenAt <= :now
                   and s.lastSeenAt > :idleCutoff
                """)
                .setParameter("sessionId", sessionId)
                .setParameter("userId", userId)
                .setParameter("now", now)
                .setParameter("idleCutoff", now.minus(idleTimeout))
                .executeUpdate();

        return updated == 1;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void revoke(UUID sessionId, Instant now) {
        requireUuidV7(sessionId, "sessionId");

        AuthSessionEntity entity = entityManager.find(
                AuthSessionEntity.class,
                sessionId);

        if (entity == null) {
            throw new IllegalStateException(
                    "Session must exist before revocation");
        }

        entity.revoke(now);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<AuthSession> findByIdForUpdate(UUID id) {
        UUID sessionId = requireUuidV7(id, "id");

        AuthSessionEntity entity = entityManager.find(
                AuthSessionEntity.class,
                sessionId,
                LockModeType.PESSIMISTIC_WRITE);

        return Optional.ofNullable(entity)
                .map(AuthSessionEntity::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int revokeAllActiveByUserId(UUID userId, Instant now) {
        // Few sessions per user: load and revoke through the entity so the domain's revokedAt rule applies.
        List<AuthSessionEntity> sessions = entityManager.createQuery(
                        "select s from AuthSessionEntity s where s.userId = :userId and s.revokedAt is null",
                        AuthSessionEntity.class)
                .setParameter("userId", userId)
                .getResultList();
        sessions.forEach(session -> session.revoke(now));
        return sessions.size();
    }
}
