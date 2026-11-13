package com.longdnh.onward.identity.infrastructure.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Account owns the exchange and declares it. The Notification service owns its queue, its binding and its
 * dead-letter queue and declares those itself (declaring is idempotent on both sides). Until that queue
 * exists, published messages are dropped by the exchange, which is acceptable for mails that can be
 * re-requested.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "rabbit")
@EnableConfigurationProperties(AuthMessagingProperties.class)
public class RabbitMessagingConfiguration {

    @Bean
    TopicExchange authExchange(AuthMessagingProperties properties) {
        return ExchangeBuilder.topicExchange(properties.exchange()).durable(true).build();
    }

    /**
     * Spring AMQP normally declares the exchange lazily, on the first connection, which here would be the
     * first publish. Declaring at startup instead makes the exchange visible before any user registers, and
     * reports a misconfigured broker in the startup log rather than in the first lost notification.
     */
    @Bean
    ApplicationRunner declareAuthExchange(AmqpAdmin amqpAdmin, TopicExchange authExchange) {
        return args -> {
            try {
                amqpAdmin.declareExchange(authExchange);
                log.info("Declared RabbitMQ exchange '{}'", authExchange.getName());
            } catch (AmqpException exception) {
                // Not fatal: the broker may come up later and the exchange is then declared on first use.
                log.warn("RabbitMQ unavailable at startup; exchange '{}' not declared yet: {}",
                        authExchange.getName(), exception.getClass().getName());
            }
        };
    }
}
