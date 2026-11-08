package com.longdnh.onward.identity.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.onward.common.events.UserDisabledEvent;
import com.longdnh.onward.identity.application.exception.InvalidCredentialsException;
import com.longdnh.onward.identity.application.exception.OwnershipMustBeTransferredException;
import com.longdnh.onward.identity.application.exception.SelfActionNotAllowedException;
import com.longdnh.onward.identity.application.exception.UserNotFoundException;
import com.longdnh.onward.identity.application.port.AuthSessionRepository;
import com.longdnh.onward.identity.application.port.OrganizationOwnershipCheck;
import com.longdnh.onward.identity.application.port.PasswordHasher;
import com.longdnh.onward.identity.application.port.UserRepository;
import com.longdnh.onward.identity.domain.User;

import lombok.RequiredArgsConstructor;

/** Account lifecycle: disable (self or admin), enable, system-admin promotion. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final OrganizationOwnershipCheck ownershipCheck;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional(readOnly = true)
    public User get(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    /** Re-authenticates with the password so a leaked access token alone cannot kill the account. */
    @Transactional
    public void disableSelf(UUID userId, String rawPassword) {
        User user = userRepository.findByIdForUpdate(userId).orElseThrow(UserNotFoundException::new);
        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        disable(user);
    }

    @Transactional
    public User disableByAdmin(UUID adminUserId, UUID targetUserId) {
        if (adminUserId.equals(targetUserId)) {
            throw new SelfActionNotAllowedException();
        }
        User user = userRepository.findByIdForUpdate(targetUserId).orElseThrow(UserNotFoundException::new);
        return disable(user);
    }

    @Transactional
    public User enable(UUID targetUserId) {
        User user = userRepository.findByIdForUpdate(targetUserId).orElseThrow(UserNotFoundException::new);
        if (user.isActive()) {
            return user;
        }
        return userRepository.save(user.enable(clock.instant()));
    }

    @Transactional
    public User promoteToSystemAdmin(UUID targetUserId) {
        User user = userRepository.findByIdForUpdate(targetUserId).orElseThrow(UserNotFoundException::new);
        if (user.isSystemAdmin()) {
            return user;
        }
        return userRepository.save(user.promoteToSystemAdmin(clock.instant()));
    }

    /**
     * Caller holds the user row lock (FOR UPDATE). Order: precondition from tenancy through a port,
     * then the identity writes, then the event whose listener (tenancy) runs in this same transaction.
     * Add-member and ownership transfer take a FOR SHARE lock on the target user first, so while this
     * lock is held nobody can make this user an owner — the ownership check cannot go stale.
     */
    private User disable(User user) {
        if (!user.isActive()) {
            return user; // idempotent
        }
        if (!ownershipCheck.organizationsOwnedBy(user.getId()).isEmpty()) {
            throw new OwnershipMustBeTransferredException();
        }
        Instant now = clock.instant();
        User disabled = userRepository.save(user.disable(now));
        authSessionRepository.revokeAllActiveByUserId(user.getId(), now);
        events.publishEvent(new UserDisabledEvent(user.getId()));
        return disabled;
    }
}
