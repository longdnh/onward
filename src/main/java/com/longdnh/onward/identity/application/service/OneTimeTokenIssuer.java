package com.longdnh.onward.identity.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.onward.common.id.IdGenerator;
import com.longdnh.onward.identity.application.port.OneTimeTokenGenerator;
import com.longdnh.onward.identity.application.port.OneTimeTokenRepository;
import com.longdnh.onward.identity.domain.OneTimeToken;
import com.longdnh.onward.identity.domain.OneTimeTokenPurpose;

import lombok.RequiredArgsConstructor;

/**
 * Issues the raw secret for an email link and stores its hash. Shared by verification and password reset.
 * The caller holds the user row lock and the transaction; the raw token is returned once and this service
 * stores only its hash (it may still travel to the Notification service through the broker).
 */
@Component
@RequiredArgsConstructor
public class OneTimeTokenIssuer {

    public record Issued(String rawToken, Instant expiresAt) {
        @Override
        public String toString() {
            return "Issued[expiresAt=" + expiresAt + "]";
        }
    }

    private final OneTimeTokenRepository tokenRepository;
    private final OneTimeTokenGenerator tokenGenerator;
    private final IdGenerator idGenerator;

    /**
     * Returns empty when the account received a token of this purpose within {@code cooldown}; the caller then
     * sends nothing and still answers 202, so the response does not reveal whether a mail went out.
     * Older usable tokens are spent so only the newest link works.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Issued> issue(UUID userId, OneTimeTokenPurpose purpose, Duration lifetime, Duration cooldown,
                                  Instant now) {
        boolean withinCooldown = tokenRepository.findLatestCreatedAt(userId, purpose)
                .map(latest -> Duration.between(latest, now).compareTo(cooldown) < 0)
                .orElse(false);
        if (withinCooldown) {
            return Optional.empty();
        }

        tokenRepository.consumeAllUsable(userId, purpose, now);

        String rawToken = tokenGenerator.generate();
        OneTimeToken token = OneTimeToken.create(
                idGenerator.generate(), userId, purpose, tokenGenerator.hash(rawToken), now, now.plus(lifetime));
        tokenRepository.create(token);

        return Optional.of(new Issued(rawToken, token.getExpiresAt()));
    }
}
