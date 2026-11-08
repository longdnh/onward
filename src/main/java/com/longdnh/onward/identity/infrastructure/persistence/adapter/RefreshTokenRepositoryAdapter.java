package com.longdnh.onward.identity.infrastructure.persistence.adapter;

import static com.longdnh.onward.common.Validation.requireUuidV7;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.onward.identity.application.port.RefreshTokenRepository;
import com.longdnh.onward.identity.domain.RefreshToken;
import com.longdnh.onward.identity.infrastructure.persistence.entities.RefreshTokenEntity;

import jakarta.persistence.EntityManager;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final EntityManager entityManager;

    public RefreshTokenRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public RefreshToken create(RefreshToken refreshToken) {
        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.fromDomain(refreshToken);
        entityManager.persist(refreshTokenEntity);
        return refreshTokenEntity.toDomain();
    }

    @Override
    public void consume(UUID tokenId, UUID replacementTokenId, Instant now) {
        requireUuidV7(tokenId, "tokenId");
        requireUuidV7(replacementTokenId, "replacementTokenId");

        RefreshTokenEntity entity = entityManager.find(
                RefreshTokenEntity.class,
                tokenId);

        if (entity == null) {
            throw new IllegalStateException(

                    "Refresh token must exist before consumption");
        }

        // push replaced token to database first
        entityManager.flush();

        entity.consume(replacementTokenId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findSessionIdByTokenHash(String tokenHash) {
        return entityManager.createQuery("""
                select r.sessionId
                from RefreshTokenEntity r
                where r.tokenHash = :tokenHash
                """, UUID.class)
                .setParameter("tokenHash", tokenHash)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return entityManager.createQuery("""
                select r
                from RefreshTokenEntity r
                where r.tokenHash = :tokenHash
                """, RefreshTokenEntity.class)
                .setParameter("tokenHash", tokenHash)
                .getResultStream()
                .findFirst()
                .map(RefreshTokenEntity::toDomain);
    }
}
