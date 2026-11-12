package com.longdnh.onward.identity.infrastructure.messaging;

import java.time.Instant;

/**
 * Wire format of the auth events, as the Notification service reads them. Language-neutral by design: plain
 * JSON, no Java class names anywhere (no __TypeId__ header), versioned type names, ISO-8601 instants.
 * Changing a field here changes the contract; add a new type version instead of altering an existing one.
 *
 * <p>The raw token is in the body on purpose (docs/microservice-decomposition.md, 6.1 option A): it is
 * single-use, short-lived, and the broker deletes the message once the consumer acknowledged it.
 * toString omits it so nothing logs it by accident.
 */
final class AuthEventMessages {

    static final String EMAIL_VERIFICATION_REQUESTED_TYPE = "onward.auth.email-verification-requested.v1";
    static final String PASSWORD_RESET_REQUESTED_TYPE = "onward.auth.password-reset-requested.v1";

    static final String EMAIL_VERIFICATION_REQUESTED_KEY = "auth.email-verification-requested";
    static final String PASSWORD_RESET_REQUESTED_KEY = "auth.password-reset-requested";

    private AuthEventMessages() {
    }

    /** Shared by both auth events today; split into separate records when their fields diverge. */
    record TokenMail(String messageId, String type, Instant occurredAt, String email, String token,
                     Instant expiresAt) {

        @Override
        public String toString() {
            return "TokenMail[messageId=" + messageId + ", type=" + type + "]";
        }
    }
}
