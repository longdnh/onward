package com.longdnh.onward.identity.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.longdnh.onward.identity.domain.RefreshToken;

public interface RefreshTokenRepository {

    RefreshToken create(RefreshToken refreshToken);

    void consume(UUID tokenId, UUID replacementTokenId, Instant now);

    Optional<UUID> findSessionIdByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

}
