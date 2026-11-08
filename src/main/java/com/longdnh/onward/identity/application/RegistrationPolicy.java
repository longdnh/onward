package com.longdnh.donot_get_left_behind.identity.application;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;
import com.longdnh.donot_get_left_behind.identity.application.exception.InvalidRegistrationException;
import com.longdnh.donot_get_left_behind.identity.domain.User;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;

public final class RegistrationPolicy {

    public static final int MIN_PASSWORD_LENGTH = 10;
    public static final int MAX_PASSWORD_LENGTH = 128;

    private final Validator validator;
    private final Set<String> blockedPasswords;

    public RegistrationPolicy(Validator validator, Set<String> blockedPasswords) {
        this.validator = Objects.requireNonNull(validator, "validator is required");
        this.blockedPasswords = Objects.requireNonNull(blockedPasswords, "blockedPasswords is required")
                .stream()
                .map(password -> password.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        if (this.blockedPasswords.isEmpty()) {
            throw new IllegalArgumentException("The common-password blocklist must not be empty");
        }
    }

    public String canonicalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidRegistrationException(ClientMessages.EMAIL_REQUIRED);
        }
        String canonicalEmail = email.strip().toLowerCase(Locale.ROOT);
        if (canonicalEmail.codePointCount(0, canonicalEmail.length()) > User.MAX_EMAIL_LENGTH) {
            throw new InvalidRegistrationException(
                    String.format(Locale.ROOT, ClientMessages.EMAIL_TOO_LONG, User.MAX_EMAIL_LENGTH));
        }
        if (!validator.validate(new EmailInput(canonicalEmail)).isEmpty()) {
            throw new InvalidRegistrationException(ClientMessages.EMAIL_INVALID);
        }
        return canonicalEmail;
    }

    public void validatePassword(String password) {
        if (password == null) {
            throw new InvalidRegistrationException(ClientMessages.PASSWORD_REQUIRED);
        }
        int length = password.codePointCount(0, password.length());
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            throw new InvalidRegistrationException(String.format(Locale.ROOT,
                    ClientMessages.PASSWORD_LENGTH_OUT_OF_RANGE, MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH));
        }
        // Case folding is only for the blocklist comparison; the original password is hashed.
        if (blockedPasswords.contains(password.toLowerCase(Locale.ROOT))) {
            throw new InvalidRegistrationException(ClientMessages.PASSWORD_TOO_COMMON);
        }
    }

    private record EmailInput(@Email String value) {
    }
}
