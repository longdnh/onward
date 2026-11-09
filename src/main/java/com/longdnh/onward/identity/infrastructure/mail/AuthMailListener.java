package com.longdnh.onward.identity.infrastructure.mail;

import java.time.Clock;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.longdnh.onward.identity.application.event.EmailVerificationRequested;
import com.longdnh.onward.identity.application.event.PasswordResetRequested;
import com.longdnh.onward.identity.application.port.EmailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends the mails that services asked for, only once their transaction committed: a rolled-back
 * registration never emails a link to a token that does not exist. Fire-and-forget by decision: if SMTP
 * fails here the account and token still exist and the user can ask for a resend. An outbox table plus a
 * retrying relay would be the production-grade upgrade. Nothing here logs the raw token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthMailListener {

    private final EmailSender emailSender;
    private final AuthMailComposer composer;
    private final Clock clock;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EmailVerificationRequested event) {
        send("verification", () -> composer.verification(event, clock.instant()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PasswordResetRequested event) {
        send("password reset", () -> composer.passwordReset(event, clock.instant()));
    }

    private void send(String kind, java.util.function.Supplier<com.longdnh.onward.identity.application.dto.EmailMessage> message) {
        try {
            emailSender.send(message.get());
        } catch (RuntimeException exception) {
            // Async: nobody is waiting for this exception, so it must be logged here or it is lost.
            log.error("Failed to send {} email: {}", kind, exception.getClass().getName());
        }
    }
}
