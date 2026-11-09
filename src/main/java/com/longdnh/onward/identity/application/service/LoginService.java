package com.longdnh.onward.identity.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;

import org.springframework.transaction.support.TransactionTemplate;

import com.longdnh.onward.identity.application.exception.EmailNotVerifiedException;
import com.longdnh.onward.identity.application.exception.InvalidCredentialsException;
import com.longdnh.onward.identity.application.dto.LoginResult;
import com.longdnh.onward.identity.application.port.AccessTokenIssuer;
import com.longdnh.onward.identity.application.port.AuthSessionRepository;
import com.longdnh.onward.common.id.IdGenerator;
import com.longdnh.onward.identity.application.port.PasswordHasher;
import com.longdnh.onward.identity.application.port.RefreshTokenGenerator;
import com.longdnh.onward.identity.application.port.RefreshTokenRepository;
import com.longdnh.onward.identity.application.port.UserRepository;
import com.longdnh.onward.identity.domain.AuthSession;
import com.longdnh.onward.identity.domain.RefreshToken;
import com.longdnh.onward.identity.domain.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenIssuer accessTokenIssuer;
    private final IdGenerator idGenerator;
    private final Clock clock;

    private final Duration accessTokenLifetime;
    private final Duration absoluteLifetime;
    private final String dummyPasswordHash;
    private final TransactionTemplate transactionTemplate;

    public LoginResult login(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null) {
            throw new InvalidCredentialsException();
        }

        String canonicalEmail = email.strip().toLowerCase(Locale.ROOT);

        if (canonicalEmail.codePointCount(0, canonicalEmail.length()) > User.MAX_EMAIL_LENGTH) {
            throw new InvalidCredentialsException();
        }

        var candidate = userRepository.findByEmail(canonicalEmail);

        String storeHash = candidate.map(User::getPasswordHash).orElse(dummyPasswordHash);

        boolean passwordMatches = passwordHasher.matches(rawPassword, storeHash);

        if (candidate.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        User verifiedUser = candidate.get();

        if (!verifiedUser.isActive()) {
            throw new InvalidCredentialsException();
        }

        // Only reached with the correct password, so this code leaks nothing to an outsider.
        if (!verifiedUser.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        LoginResult result = transactionTemplate.execute(
                status -> createLogin(verifiedUser));

        return Objects.requireNonNull(result);

    }

    private LoginResult createLogin(User verifiedUser) {
        User currentUser = userRepository.findById(verifiedUser.getId())
                .orElseThrow(InvalidCredentialsException::new);

        if (!currentUser.canAuthenticate() || !currentUser.getPasswordHash().equals(verifiedUser.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);

        AuthSession session = AuthSession.create(idGenerator.generate(), verifiedUser.getId(), now,
                now.plus(absoluteLifetime));

        String rawRefreshToken = refreshTokenGenerator.generate();
        RefreshToken refreshToken = RefreshToken.create(
                idGenerator.generate(),
                session.getId(),
                refreshTokenGenerator.hash(rawRefreshToken),
                now,
                session.getAbsoluteExpiresAt());

        Instant accessExpiresAt = now.plus(accessTokenLifetime);

        if (accessExpiresAt.isAfter(session.getAbsoluteExpiresAt())) {
            accessExpiresAt = session.getAbsoluteExpiresAt();
        }

        String accessToken = accessTokenIssuer.issue(
                currentUser.getId(),
                session.getId(),
                now,
                accessExpiresAt);

        sessionRepository.create(session);
        refreshTokenRepository.create(refreshToken);

        return new LoginResult(
                accessToken,
                accessExpiresAt,
                rawRefreshToken,
                refreshToken.getExpiresAt());
    }
}
