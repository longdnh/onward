package com.longdnh.onward.identity.infrastructure.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.longdnh.onward.identity.application.dto.EmailMessage;
import com.longdnh.onward.identity.application.port.EmailSender;

/** Sends through the JavaMailSender that Spring Boot builds from spring.mail.* (Mailpit locally). */
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender, AuthMailProperties properties) {
        this.mailSender = mailSender;
        this.from = properties.from();
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.textBody());
        mailSender.send(mail);
    }
}
