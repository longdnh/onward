package com.longdnh.donot_get_left_behind.identity.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.support.TransactionTemplate;

import com.longdnh.donot_get_left_behind.identity.application.port.AuthSessionRepository;
import com.longdnh.donot_get_left_behind.identity.application.port.RefreshTokenGenerator;
import com.longdnh.donot_get_left_behind.identity.application.port.RefreshTokenRepository;
import com.longdnh.donot_get_left_behind.identity.domain.AuthSession;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LogoutService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthSessionRepository authSessionRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public void logout(String rawToken) {
        if (rawToken == null) {
            return;
        }

        String hash = refreshTokenGenerator.hash(rawToken);
        transactionTemplate.executeWithoutResult(status -> {
            Optional<UUID> sessionId = refreshTokenRepository.findSessionIdByTokenHash(hash);
            if (sessionId.isEmpty()) {
                return;
            }
            Optional<AuthSession> session = authSessionRepository.findByIdForUpdate(sessionId.get());
            if (session.isEmpty()) {
                return;
            }

            if (session.get().isRevoked()) {
                return;
            }

            Instant now = clock.instant();
            Instant revokedAt = now.isBefore(session.get().getLastSeenAt())
                    ? session.get().getLastSeenAt()
                    : now;
            authSessionRepository.revoke(session.get().getId(), revokedAt);

        });

    }
}
