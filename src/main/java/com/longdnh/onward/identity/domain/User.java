package com.longdnh.onward.identity.domain;

import static com.longdnh.onward.common.Validation.requireNonBlank;
import static com.longdnh.onward.common.Validation.requireUuidV7;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

@Getter
public final class User {

    public static final int MAX_EMAIL_LENGTH = 254;

    public enum UserStatus {
        ACTIVE,
        DISABLED
    }

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    /** Null for ordinary users. */
    private final SystemRole systemRole;
    /**
     * Null until the user proves ownership of the mailbox. Orthogonal to {@link #status}: status is the
     * admin-managed lifecycle, this is evidence. Login requires both ({@link #canAuthenticate()}).
     */
    private final Instant emailVerifiedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Reconstitutes a user from existing state. The password must already be hashed.
     */
    public User(UUID id, String email, String passwordHash, UserStatus status, SystemRole systemRole,
                Instant emailVerifiedAt, Instant createdAt, Instant updatedAt) {
        this.id = requireUuidV7(id, "id");
        this.email = requireNonBlank(email, "email").strip().toLowerCase(Locale.ROOT);
        if (this.email.codePointCount(0, this.email.length()) > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("email must not exceed " + MAX_EMAIL_LENGTH + " characters");
        }
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status, "status is required");
        this.systemRole = systemRole;
        this.emailVerifiedAt = emailVerifiedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
        if (emailVerifiedAt != null && emailVerifiedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("emailVerifiedAt must not precede createdAt");
        }
    }

    /** New accounts are active but unverified; verification is a separate step driven by email. */
    public static User create(UUID id, String email, String passwordHash, Instant now) {
        return new User(id, email, passwordHash, UserStatus.ACTIVE, null, null, now, now);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    /** The condition for login, refresh and accepting an access token: enabled by admins and verified by the user. */
    public boolean canAuthenticate() {
        return isActive() && isEmailVerified();
    }

    public boolean isSystemAdmin() {
        return systemRole == SystemRole.ADMIN;
    }

    /** State only; the application service checks ownership, verifies credentials and revokes sessions. */
    public User disable(Instant changedAt) {
        return changed(UserStatus.DISABLED, systemRole, passwordHash, emailVerifiedAt, changedAt);
    }

    public User enable(Instant changedAt) {
        return changed(UserStatus.ACTIVE, systemRole, passwordHash, emailVerifiedAt, changedAt);
    }

    public User promoteToSystemAdmin(Instant changedAt) {
        return changed(status, SystemRole.ADMIN, passwordHash, emailVerifiedAt, changedAt);
    }

    /** Idempotent: a second verification keeps the first timestamp. */
    public User verifyEmail(Instant changedAt) {
        if (isEmailVerified()) {
            return this;
        }
        return changed(status, systemRole, passwordHash, changedAt, changedAt);
    }

    /** The new password must already be hashed; the application service revokes sessions. */
    public User changePassword(String newPasswordHash, Instant changedAt) {
        return changed(status, systemRole, requireNonBlank(newPasswordHash, "newPasswordHash"), emailVerifiedAt,
                changedAt);
    }

    private User changed(UserStatus newStatus, SystemRole newRole, String newPasswordHash, Instant newEmailVerifiedAt,
                         Instant changedAt) {
        Objects.requireNonNull(changedAt, "changedAt is required");
        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("changedAt must not precede updatedAt");
        }
        return new User(id, email, newPasswordHash, newStatus, newRole, newEmailVerifiedAt, createdAt, changedAt);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof User user && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", status=" + status + ", emailVerified=" + isEmailVerified() + "}";
    }
}
