package com.longdnh.onward.identity.infrastructure.configuration;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated 
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(

    @NotNull @DurationMin(inclusive = false)
    Duration accessTokenLifetime,

    @NotNull @DurationMin(inclusive = false)
    Duration idleTimeout,

    @NotNull @DurationMin(inclusive = false)
    Duration absoluteLifetime,

    @NotBlank
    String issuer,

    @NotBlank
    String audience,

    /** How long an email-verification link stays valid. */
    @NotNull @DurationMin(inclusive = false)
    Duration emailVerificationTokenLifetime,

    /** How long a password-reset link stays valid; short, because it grants account takeover. */
    @NotNull @DurationMin(inclusive = false)
    Duration passwordResetTokenLifetime,

    /** Minimum gap between two emails of the same purpose to one account; a cheap anti-spam guard. */
    @NotNull @DurationMin(inclusive = false)
    Duration emailResendCooldown

) {
    @AssertTrue (message = "Access-token lifetime and idle timeout must not exceed absolute lifetime")
    public boolean isLifetimeOrderIsValid(){
          if (accessTokenLifetime == null
                || idleTimeout == null
                || absoluteLifetime == null) {
            return true;
        }

        return accessTokenLifetime.compareTo(absoluteLifetime) <= 0
                && idleTimeout.compareTo(absoluteLifetime) <= 0;
    }
}
