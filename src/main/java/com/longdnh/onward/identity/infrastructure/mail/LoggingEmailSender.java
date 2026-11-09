package com.longdnh.onward.identity.infrastructure.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.longdnh.onward.identity.application.dto.EmailMessage;
import com.longdnh.onward.identity.application.port.EmailSender;

import lombok.extern.slf4j.Slf4j;

/** Development only: writes the whole mail, link included, to the log instead of sending it. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        log.info("Email (not sent, app.mail.mode=log)\nTo: {}\nSubject: {}\n\n{}",
                message.to(), message.subject(), message.textBody());
    }
}
