package com.longdnh.onward.identity.infrastructure.mail;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.longdnh.onward.identity.application.event.EmailVerificationRequested;
import com.longdnh.onward.identity.application.event.PasswordResetRequested;
import com.longdnh.onward.identity.application.port.AuthNotifier;
import com.longdnh.onward.identity.application.port.EmailSender;

import lombok.RequiredArgsConstructor;

/**
 * Monolith path (app.mail.mode=log or smtp): composes the mail here and hands it to the EmailSender.
 * With app.mail.mode=rabbit the event is published instead and the Notification service composes the mail.
 */
@Component
@ConditionalOnExpression("'${app.mail.mode:log}' != 'rabbit'")
@RequiredArgsConstructor
public class LocalMailAuthNotifier implements AuthNotifier {

    private final EmailSender emailSender;
    private final AuthMailComposer composer;
    private final Clock clock;

    @Override
    public void emailVerificationRequested(EmailVerificationRequested event) {
        emailSender.send(composer.verification(event, clock.instant()));
    }

    @Override
    public void passwordResetRequested(PasswordResetRequested event) {
        emailSender.send(composer.passwordReset(event, clock.instant()));
    }
}
