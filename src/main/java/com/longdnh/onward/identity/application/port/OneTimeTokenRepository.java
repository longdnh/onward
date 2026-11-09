package com.longdnh.onward.identity.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.longdnh.onward.identity.domain.OneTimeToken;
import com.longdnh.onward.identity.domain.OneTimeTokenPurpose;

public interface OneTimeTokenRepository {

    OneTimeToken create(OneTimeToken token);

    Optional<OneTimeToken> findByTokenHash(String tokenHash);

    /**
     * Conditional single-statement spend: succeeds only if the token is still unconsumed and unexpired at
     * {@code now}. Two concurrent callers cannot both get {@code true}. Caller's transaction is mandatory.
     */
    boolean consumeIfUsable(UUID tokenId, Instant now);

    /** Marks every still-usable token of the user and purpose consumed, so only the newest link works. */
    int consumeAllUsable(UUID userId, OneTimeTokenPurpose purpose, Instant now);

    /** Creation time of the most recently issued token for the user and purpose; drives the resend cooldown. */
    Optional<Instant> findLatestCreatedAt(UUID userId, OneTimeTokenPurpose purpose);
}
