package com.longdnh.onward.identity.application.port;

import com.longdnh.onward.identity.application.event.EmailVerificationRequested;
import com.longdnh.onward.identity.application.event.PasswordResetRequested;

/**
 * Outbound port: hands an authentication event to whoever delivers notifications. The local adapter composes
 * and sends the mail in this process; the messaging adapter publishes the event for the Notification service,
 * which owns the templates. Implementations must never be called inside a database transaction: neither a
 * sent mail nor a published message can be rolled back, so callers invoke this after commit.
 */
public interface AuthNotifier {

    void emailVerificationRequested(EmailVerificationRequested event);

    void passwordResetRequested(PasswordResetRequested event);
}
