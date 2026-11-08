package com.longdnh.donot_get_left_behind.identity.infrastructure.configuration;

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
    String audience

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
