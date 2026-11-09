package com.longdnh.onward.identity.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.longdnh.onward.identity.application.RegistrationPolicy;
import com.longdnh.onward.identity.application.event.EmailVerificationRequested;
import com.longdnh.onward.identity.application.exception.InvalidTokenException;
import com.longdnh.onward.identity.application.port.OneTimeTokenGenerator;
import com.longdnh.onward.identity.application.port.OneTimeTokenRepository;
import com.longdnh.onward.identity.application.port.UserRepository;
import com.longdnh.onward.identity.domain.OneTimeToken;
import com.longdnh.onward.identity.domain.OneTimeTokenPurpose;
import com.longdnh.onward.identity.domain.User;

import lombok.RequiredArgsConstructor;

/**
 * Proves mailbox ownership. Public entry points answer the same way whether or not the account exists:
 * resend always "succeeds", and verify fails with one generic error for every bad token.
 * Wired in IdentityConfiguration; durations come from AuthProperties without this class importing it.
 */
@RequiredArgsConstructor
public class EmailVerificationService {

    /** 32 random bytes in unpadded base64url. Anything else is rejected before touching the database. */
    static final Pattern RAW_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final UserRepository userRepository;
    private final OneTimeTokenRepository tokenRepository;
    private final OneTimeTokenGenerator tokenGenerator;
    private final OneTimeTokenIssuer tokenIssuer;
    private final RegistrationPolicy registrationPolicy;
    private final ApplicationEventPublisher events;
    private final Duration tokenLifetime;
    private final Duration resendCooldown;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * Issues a verification token for the user and publishes the event that sends the mail after commit.
     * Does nothing for disabled or already verified accounts, or while the resend cooldown is running.
     * Must run inside the caller's transaction (register, resend) so token and account commit together.
     */
    public void sendVerification(UUID userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("sendVerification requires an active transaction");
        }
        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null || !user.isActive() || user.isEmailVerified()) {
            return;
        }
        Instant now = clock.instant();
        tokenIssuer.issue(user.getId(), OneTimeTokenPurpose.EMAIL_VERIFICATION, tokenLifetime, resendCooldown, now)
                .ifPresent(issued -> events.publishEvent(
                        new EmailVerificationRequested(user.getEmail(), issued.rawToken(), issued.expiresAt())));
    }

    /** Public resend. Unknown email, verified account, disabled account and cooldown all look identical. */
    public void resend(String email) {
        String canonicalEmail = registrationPolicy.canonicalizeEmail(email);
        transactionTemplate.executeWithoutResult(status ->
                userRepository.findByEmail(canonicalEmail).ifPresent(user -> sendVerification(user.getId())));
    }

    public void verify(String rawToken) {
        if (rawToken == null || !RAW_TOKEN_PATTERN.matcher(rawToken).matches()) {
            throw new InvalidTokenException();
        }
        String tokenHash = tokenGenerator.hash(rawToken);
        transactionTemplate.executeWithoutResult(status -> verifyInTransaction(tokenHash));
    }

    private void verifyInTransaction(String tokenHash) {
        OneTimeToken token = tokenRepository.findByTokenHash(tokenHash)
                .filter(candidate -> candidate.getPurpose() == OneTimeTokenPurpose.EMAIL_VERIFICATION)
                .orElseThrow(InvalidTokenException::new);

        // Lock order: user row first, then the token row. Time is read after the lock is held.
        User user = userRepository.findByIdForUpdate(token.getUserId()).orElseThrow(InvalidTokenException::new);
        Instant now = clock.instant();

        if (!tokenRepository.consumeIfUsable(token.getId(), now)) {
            throw new InvalidTokenException();
        }
        if (!user.isActive()) {
            throw new InvalidTokenException();
        }
        if (!user.isEmailVerified()) {
            userRepository.save(user.verifyEmail(now));
        }
    }
}
