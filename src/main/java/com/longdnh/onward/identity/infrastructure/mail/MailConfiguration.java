package com.longdnh.onward.identity.infrastructure.mail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @EnableAsync lets the after-commit mail listener run on Boot's applicationTaskExecutor, so the HTTP
 * response does not wait on SMTP. That also keeps the public endpoints' response time independent of
 * whether a mail was actually sent.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties(AuthMailProperties.class)
public class MailConfiguration {
}
