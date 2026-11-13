package com.longdnh.onward.identity.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * exchange: the durable topic exchange auth events are published to. The Notification service binds its own
 * queue to it; the exchange name and the routing keys in AuthEventMessages are the contract between them.
 */
@Validated
@ConfigurationProperties(prefix = "app.messaging")
public record AuthMessagingProperties(@NotBlank @DefaultValue("onward.auth") String exchange) {
}
