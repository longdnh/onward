package com.longdnh.onward.identity.infrastructure.mail;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.longdnh.onward.identity.application.dto.EmailMessage;
import com.longdnh.onward.identity.application.event.EmailVerificationRequested;
import com.longdnh.onward.identity.application.event.PasswordResetRequested;

/** Plain-text mail bodies. The token goes in the URL query only; the frontend page POSTs it to the API. */
@Component
public class AuthMailComposer {

    private final String frontendBaseUrl;

    public AuthMailComposer(AuthMailProperties properties) {
        String base = properties.frontendBaseUrl();
        this.frontendBaseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public EmailMessage verification(EmailVerificationRequested event, Instant now) {
        String link = link("/verify-email", event.rawToken());
        String body = """
                Welcome to Onward.

                Confirm your email address by opening this link:
                %s

                The link is valid for %s. If you did not create an account, ignore this message.
                """.formatted(link, humanDuration(Duration.between(now, event.expiresAt())));
        return new EmailMessage(event.email(), "Confirm your email address", body);
    }

    public EmailMessage passwordReset(PasswordResetRequested event, Instant now) {
        String link = link("/reset-password", event.rawToken());
        String body = """
                A password reset was requested for your Onward account.

                Choose a new password by opening this link:
                %s

                The link is valid for %s and can be used once. If you did not request this, ignore this message;
                your password stays unchanged.
                """.formatted(link, humanDuration(Duration.between(now, event.expiresAt())));
        return new EmailMessage(event.email(), "Reset your password", body);
    }

    private String link(String path, String rawToken) {
        return frontendBaseUrl + path + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private static String humanDuration(Duration duration) {
        // The listener runs after commit, so a 24h token shows up as 23h59m59s here: round up to whole minutes.
        long minutes = Math.max(1, (duration.toSeconds() + 59) / 60);
        if (minutes % 60 == 0) {
            long hours = minutes / 60;
            return String.format(Locale.ROOT, "%d hour%s", hours, hours == 1 ? "" : "s");
        }
        return String.format(Locale.ROOT, "%d minute%s", minutes, minutes == 1 ? "" : "s");
    }
}
