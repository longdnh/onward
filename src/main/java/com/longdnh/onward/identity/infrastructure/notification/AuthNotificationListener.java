package com.longdnh.onward.identity.infrastructure.notification;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.longdnh.onward.identity.application.event.EmailVerificationRequested;
import com.longdnh.onward.identity.application.event.PasswordResetRequested;
import com.longdnh.onward.identity.application.port.AuthNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Forwards auth events to the AuthNotifier only once their transaction committed: a rolled-back registration
 * never produces a link to a token that does not exist. Fire-and-forget by decision (see
 * docs/microservice-decomposition.md, 6.2): if the mail or the broker fails here the account and token still
 * exist and the user can ask for a resend. A crash between commit and this listener loses the notification;
 * an outbox is reserved for events whose loss would be a bug. Nothing here logs the raw token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthNotificationListener {

    private final AuthNotifier notifier;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EmailVerificationRequested event) {
        deliver("email verification", () -> notifier.emailVerificationRequested(event));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PasswordResetRequested event) {
        deliver("password reset", () -> notifier.passwordResetRequested(event));
    }

    private void deliver(String kind, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            // Async: nobody is waiting for this exception, so it must be logged here or it is lost.
            log.error("Failed to deliver {} notification: {}", kind, exception.getClass().getName());
        }
    }
}
