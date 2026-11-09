package com.longdnh.onward.identity.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import com.longdnh.onward.identity.application.RegistrationPolicy;
import com.longdnh.onward.identity.application.event.PasswordResetRequested;
import com.longdnh.onward.identity.application.exception.InvalidTokenException;
import com.longdnh.onward.identity.application.port.AuthSessionRepository;
import com.longdnh.onward.identity.application.port.OneTimeTokenGenerator;
import com.longdnh.onward.identity.application.port.OneTimeTokenRepository;
import com.longdnh.onward.identity.application.port.PasswordHasher;
import com.longdnh.onward.identity.application.port.UserRepository;
import com.longdnh.onward.identity.domain.OneTimeToken;
import com.longdnh.onward.identity.domain.OneTimeTokenPurpose;
import com.longdnh.onward.identity.domain.User;

import lombok.RequiredArgsConstructor;

/**
 * Forgot/reset password. The request step never reveals whether the email is registered. The reset step
 * spends the token, replaces the hash and revokes every session in one transaction, so a stolen session
 * does not survive the owner regaining control. Wired in IdentityConfiguration.
 */
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final OneTimeTokenRepository tokenRepository;
    private final OneTimeTokenGenerator tokenGenerator;
    private final OneTimeTokenIssuer tokenIssuer;
    private final AuthSessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;
    private final RegistrationPolicy registrationPolicy;
    private final ApplicationEventPublisher events;
    private final Duration tokenLifetime;
    private final Duration resendCooldown;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * Only accounts that could log in get a reset mail. An unverified account has no proven mailbox yet;
     * it must finish verification first. A disabled account was turned off by an admin on purpose.
     */
    public void requestReset(String email) {
        String canonicalEmail = registrationPolicy.canonicalizeEmail(email);
        transactionTemplate.executeWithoutResult(status -> {
            User candidate = userRepository.findByEmail(canonicalEmail).orElse(null);
            if (candidate == null) {
                return;
            }
            User user = userRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (user == null || !user.canAuthenticate()) {
                return;
            }
            Instant now = clock.instant();
            tokenIssuer.issue(user.getId(), OneTimeTokenPurpose.PASSWORD_RESET, tokenLifetime, resendCooldown, now)
                    .ifPresent(issued -> events.publishEvent(
                            new PasswordResetRequested(user.getEmail(), issued.rawToken(), issued.expiresAt())));
        });
    }

    public void reset(String rawToken, String newPassword) {
        if (rawToken == null || !EmailVerificationService.RAW_TOKEN_PATTERN.matcher(rawToken).matches()) {
            throw new InvalidTokenException();
        }
        registrationPolicy.validatePassword(newPassword);
        // Slow hash outside the transaction, like registration and login.
        String newPasswordHash = passwordHasher.hash(newPassword);
        String tokenHash = tokenGenerator.hash(rawToken);
        transactionTemplate.executeWithoutResult(status -> resetInTransaction(tokenHash, newPasswordHash));
    }

    private void resetInTransaction(String tokenHash, String newPasswordHash) {
        OneTimeToken token = tokenRepository.findByTokenHash(tokenHash)
                .filter(candidate -> candidate.getPurpose() == OneTimeTokenPurpose.PASSWORD_RESET)
                .orElseThrow(InvalidTokenException::new);

        User user = userRepository.findByIdForUpdate(token.getUserId()).orElseThrow(InvalidTokenException::new);
        Instant now = clock.instant();

        if (!tokenRepository.consumeIfUsable(token.getId(), now)) {
            throw new InvalidTokenException();
        }
        if (!user.isActive()) {
            throw new InvalidTokenException();
        }
        // Completing a reset proves mailbox ownership as well, so an unverified flag is cleared too.
        userRepository.save(user.changePassword(newPasswordHash, now).verifyEmail(now));
        sessionRepository.revokeAllActiveByUserId(user.getId(), now);
    }
}
