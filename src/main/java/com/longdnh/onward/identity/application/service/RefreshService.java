package com.longdnh.donot_get_left_behind.identity.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.transaction.support.TransactionTemplate;

import com.longdnh.donot_get_left_behind.identity.application.exception.InvalidRefreshTokenException;
import com.longdnh.donot_get_left_behind.identity.application.dto.RefreshResult;
import com.longdnh.donot_get_left_behind.identity.application.port.AccessTokenIssuer;
import com.longdnh.donot_get_left_behind.identity.application.port.AuthSessionRepository;
import com.longdnh.donot_get_left_behind.common.id.IdGenerator;
import com.longdnh.donot_get_left_behind.identity.application.port.RefreshTokenGenerator;
import com.longdnh.donot_get_left_behind.identity.application.port.RefreshTokenRepository;
import com.longdnh.donot_get_left_behind.identity.application.port.UserRepository;
import com.longdnh.donot_get_left_behind.identity.domain.RefreshToken;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RefreshService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final UserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenIssuer accessTokenIssuer;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final Duration accessTokenLifetime;
    private final Duration idleTimeout;
    private final TransactionTemplate transactionTemplate;

    public RefreshResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null
                || !TOKEN_PATTERN.matcher(rawRefreshToken).matches()) {
            throw new InvalidRefreshTokenException();
        }

        String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);

        Optional<RefreshResult> outcome = Objects.requireNonNull(
                transactionTemplate.execute(
                        status -> refreshInTransaction(tokenHash)));

        // Transaction đã kết thúc trước khi xử lý kết quả ở đây.
        return outcome.orElseThrow(InvalidRefreshTokenException::new);
    }

    private Optional<RefreshResult> refreshInTransaction(String tokenHash) {
        var sessionId = refreshTokenRepository.findSessionIdByTokenHash(tokenHash);

        if (sessionId.isEmpty()) {
            return Optional.empty();
        }

        var sessionCandidate = sessionRepository.findByIdForUpdate(sessionId.get());

        if (sessionCandidate.isEmpty()) {
            return Optional.empty();
        }

        var session = sessionCandidate.get();

        // Chỉ đọc trạng thái token sau khi đã lấy được khóa session.
        var tokenCandidate = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenCandidate.isEmpty()) {
            return Optional.empty();
        }

        var currentToken = tokenCandidate.get();

        if (!currentToken.getSessionId().equals(session.getId())) {
            return Optional.empty();
        }

        // Lấy thời gian sau khi chờ khóa.
        Instant now = clock.instant();

        if (currentToken.isConsumed()) {
            // Giữ timestamp thu hồi không đứng trước lastSeenAt.
            Instant revokedAt = now.isBefore(session.getLastSeenAt())
                    ? session.getLastSeenAt()
                    : now;

            sessionRepository.revoke(session.getId(), revokedAt);

            // Trả kết quả bình thường để transaction commit việc thu hồi.
            return Optional.empty();
        }

        if (!currentToken.isUsableAt(now)
                || !session.isActiveAt(now, idleTimeout)) {
            return Optional.empty();
        }

        var userCandidate = userRepository.findById(session.getUserId());

        if (userCandidate.isEmpty()
                || !userCandidate.get().isActive()) {
            return Optional.empty();
        }

        Instant issuedAt = now.truncatedTo(ChronoUnit.SECONDS);
        Instant accessExpiresAt = now.plus(accessTokenLifetime)
                .truncatedTo(ChronoUnit.SECONDS);

        Instant sessionDeadline = session.getAbsoluteExpiresAt()
                .truncatedTo(ChronoUnit.SECONDS);

        if (accessExpiresAt.isAfter(sessionDeadline)) {
            accessExpiresAt = sessionDeadline;
        }

        // Không phát JWT đã hết hạn hoặc còn dưới độ chính xác một giây.
        if (!accessExpiresAt.isAfter(now)) {
            return Optional.empty();
        }

        String newRawRefreshToken = refreshTokenGenerator.generate();

        RefreshToken replacement = RefreshToken.create(
                idGenerator.generate(),
                session.getId(),
                refreshTokenGenerator.hash(newRawRefreshToken),
                now,
                session.getAbsoluteExpiresAt());

        String accessToken = accessTokenIssuer.issue(
                session.getUserId(),
                session.getId(),
                issuedAt,
                accessExpiresAt);

        refreshTokenRepository.create(replacement);

        refreshTokenRepository.consume(
                currentToken.getId(),
                replacement.getId(),
                now);

        boolean recorded = sessionRepository.recordActivityIfActive(
                session.getId(),
                session.getUserId(),
                now,
                idleTimeout);

        if (!recorded) {
            // Nhánh này phải rollback cả INSERT B và consume A.
            throw new InvalidRefreshTokenException();
        }

        return Optional.of(new RefreshResult(
                accessToken,
                accessExpiresAt,
                newRawRefreshToken,
                replacement.getExpiresAt()));
    }
}