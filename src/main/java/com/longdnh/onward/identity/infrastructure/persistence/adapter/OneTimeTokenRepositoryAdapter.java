package com.longdnh.onward.identity.infrastructure.persistence.adapter;

import static com.longdnh.onward.common.Validation.requireNonBlank;
import static com.longdnh.onward.common.Validation.requireUuidV7;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.onward.identity.application.port.OneTimeTokenRepository;
import com.longdnh.onward.identity.domain.OneTimeToken;
import com.longdnh.onward.identity.domain.OneTimeTokenPurpose;
import com.longdnh.onward.identity.infrastructure.persistence.entities.OneTimeTokenEntity;

import jakarta.persistence.EntityManager;

/**
 * Every operation here runs inside the application service's transaction (MANDATORY): the service locks the
 * user row first, then touches tokens, keeping the system-wide lock order user → everything else.
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class OneTimeTokenRepositoryAdapter implements OneTimeTokenRepository {

    private final EntityManager entityManager;

    public OneTimeTokenRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public OneTimeToken create(OneTimeToken token) {
        OneTimeTokenEntity entity = OneTimeTokenEntity.fromDomain(token);
        entityManager.persist(entity);
        return entity.toDomain();
    }

    @Override
    public Optional<OneTimeToken> findByTokenHash(String tokenHash) {
        return entityManager.createQuery("""
                select t from OneTimeTokenEntity t where t.tokenHash = :tokenHash
                """, OneTimeTokenEntity.class)
                .setParameter("tokenHash", requireNonBlank(tokenHash, "tokenHash"))
                .getResultStream()
                .findFirst()
                .map(OneTimeTokenEntity::toDomain);
    }

    @Override
    public boolean consumeIfUsable(UUID tokenId, Instant now) {
        requireUuidV7(tokenId, "tokenId");
        // Same pattern as AuthSessionRepositoryAdapter.recordActivityIfActive: the WHERE clause is the check,
        // the row count is the answer, and the database serializes concurrent spenders.
        int updated = entityManager.createQuery("""
                update OneTimeTokenEntity t
                   set t.consumedAt = :now
                 where t.id = :tokenId
                   and t.consumedAt is null
                   and t.createdAt <= :now
                   and t.expiresAt > :now
                """)
                .setParameter("tokenId", tokenId)
                .setParameter("now", now)
                .executeUpdate();
        return updated == 1;
    }

    @Override
    public int consumeAllUsable(UUID userId, OneTimeTokenPurpose purpose, Instant now) {
        requireUuidV7(userId, "userId");
        int updated = entityManager.createQuery("""
                update OneTimeTokenEntity t
                   set t.consumedAt = :now
                 where t.userId = :userId
                   and t.purpose = :purpose
                   and t.consumedAt is null
                   and t.expiresAt > :now
                """)
                .setParameter("userId", userId)
                .setParameter("purpose", purpose)
                .setParameter("now", now)
                .executeUpdate();
        return updated;
    }

    @Override
    public Optional<Instant> findLatestCreatedAt(UUID userId, OneTimeTokenPurpose purpose) {
        return entityManager.createQuery("""
                select max(t.createdAt) from OneTimeTokenEntity t
                 where t.userId = :userId and t.purpose = :purpose
                """, Instant.class)
                .setParameter("userId", requireUuidV7(userId, "userId"))
                .setParameter("purpose", purpose)
                .getResultStream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }
}
