package com.longdnh.onward.identity.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * mode: "log" prints the mail to the application log (default, no SMTP needed); "smtp" sends through
 * JavaMailSender, which Spring Boot configures from spring.mail.* properties; "rabbit" publishes the event to
 * RabbitMQ and lets the Notification service compose and send the mail (frontendBaseUrl is then its setting).
 * frontendBaseUrl: where the links in the mails point; the frontend extracts the token and POSTs it back.
 */
@Validated
@ConfigurationProperties(prefix = "app.mail")
public record AuthMailProperties(
        @NotBlank @Pattern(regexp = "log|smtp|rabbit") String mode,
        @NotBlank String from,
        @NotBlank @Pattern(regexp = "https?://.+", message = "must be an absolute http(s) URL") String frontendBaseUrl) {
}
