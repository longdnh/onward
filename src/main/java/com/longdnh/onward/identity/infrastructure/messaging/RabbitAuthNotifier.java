package com.longdnh.onward.identity.infrastructure.messaging;

import static com.longdnh.onward.identity.infrastructure.messaging.AuthEventMessages.EMAIL_VERIFICATION_REQUESTED_KEY;
import static com.longdnh.onward.identity.infrastructure.messaging.AuthEventMessages.EMAIL_VERIFICATION_REQUESTED_TYPE;
import static com.longdnh.onward.identity.infrastructure.messaging.AuthEventMessages.PASSWORD_RESET_REQUESTED_KEY;
import static com.longdnh.onward.identity.infrastructure.messaging.AuthEventMessages.PASSWORD_RESET_REQUESTED_TYPE;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.longdnh.onward.common.id.IdGenerator;
import com.longdnh.onward.identity.application.event.EmailVerificationRequested;
import com.longdnh.onward.identity.application.event.PasswordResetRequested;
import com.longdnh.onward.identity.application.port.AuthNotifier;
import com.longdnh.onward.identity.infrastructure.messaging.AuthEventMessages.TokenMail;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes auth events to RabbitMQ for the Notification service (app.mail.mode=rabbit). Serializes by hand
 * rather than through Spring's JSON message converter, so the message carries only the JSON body and
 * standard AMQP properties and a non-Java consumer needs nothing from this code base.
 *
 * <p>Persistent delivery means the broker writes the message to disk before routing it, so a broker restart
 * does not lose it. There are no publisher confirms: a broker outage at publish time loses the notification,
 * which is the accepted trade-off of publishing after commit (docs/microservice-decomposition.md, 6.2).
 */
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "rabbit")
@RequiredArgsConstructor
public class RabbitAuthNotifier implements AuthNotifier {

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;
    private final AuthMessagingProperties properties;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    public void emailVerificationRequested(EmailVerificationRequested event) {
        publish(EMAIL_VERIFICATION_REQUESTED_KEY, EMAIL_VERIFICATION_REQUESTED_TYPE,
                event.email(), event.rawToken(), event.expiresAt());
    }

    @Override
    public void passwordResetRequested(PasswordResetRequested event) {
        publish(PASSWORD_RESET_REQUESTED_KEY, PASSWORD_RESET_REQUESTED_TYPE,
                event.email(), event.rawToken(), event.expiresAt());
    }

    private void publish(String routingKey, String type, String email, String token, Instant expiresAt) {
        Instant now = clock.instant();
        TokenMail payload = new TokenMail(
                idGenerator.generate().toString(), type, now, email, token, expiresAt);

        Message message = MessageBuilder
                .withBody(jsonMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(payload.messageId())
                .setType(type)
                .setTimestamp(Date.from(now))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

        rabbitTemplate.send(properties.exchange(), routingKey, message);
    }
}
