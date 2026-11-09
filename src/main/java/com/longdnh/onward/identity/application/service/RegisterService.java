package com.longdnh.onward.identity.application.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.transaction.support.TransactionTemplate;

import com.longdnh.onward.common.id.IdGenerator;
import com.longdnh.onward.identity.application.RegistrationPolicy;
import com.longdnh.onward.identity.application.exception.EmailAlreadyExistsException;
import com.longdnh.onward.identity.application.port.PasswordHasher;
import com.longdnh.onward.identity.application.port.UserRepository;
import com.longdnh.onward.identity.domain.User;

import lombok.RequiredArgsConstructor;

/**
 * Creates an unverified account and queues its verification mail. The outcome is the same 202 whether the
 * email was free, already registered, or lost a concurrent race: the response never reveals existence.
 * An existing unverified account gets its verification mail again instead; a verified one gets nothing.
 */
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final IdGenerator idGenerator;
    private final PasswordHasher passwordHasher;
    private final RegistrationPolicy registrationPolicy;
    private final EmailVerificationService emailVerificationService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public void register(String email, String rawPassword) {
        String canonicalEmail = registrationPolicy.canonicalizeEmail(email);
        registrationPolicy.validatePassword(rawPassword);

        // Hash before the transaction, and for every request: the duplicate path then costs the same time.
        String passwordHash = passwordHasher.hash(rawPassword);

        try {
            transactionTemplate.executeWithoutResult(status -> {
                User existing = userRepository.findByEmail(canonicalEmail).orElse(null);
                if (existing != null) {
                    emailVerificationService.sendVerification(existing.getId());
                    return;
                }
                Instant now = clock.instant();
                User user = userRepository.create(User.create(idGenerator.generate(), canonicalEmail, passwordHash, now));
                emailVerificationService.sendVerification(user.getId());
            });
        } catch (EmailAlreadyExistsException raceLost) {
            // A concurrent registration inserted the same email between our lookup and insert. The row exists
            // and its owner receives that request's mail; nothing more to do here.
        }
    }
}
