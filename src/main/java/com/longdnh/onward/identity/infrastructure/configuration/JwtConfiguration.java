package com.longdnh.donot_get_left_behind.identity.infrastructure.configuration;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    @Bean
    public JwtEncoder jwtEncoder(
            @Value("${app.auth.jwt-private-key}") Resource privateKeyResource,
            @Value("${app.auth.jwt-public-key}") Resource publicKeyResource) throws IOException {
        RSAPrivateKey privateKey;
        RSAPublicKey publicKey;

        try (var input = privateKeyResource.getInputStream()) {
            privateKey = Objects.requireNonNull(
                    RsaKeyConverters.pkcs8().convert(input),
                    "RSA private key is required");
        }

        try (var input = publicKeyResource.getInputStream()) {
            publicKey = Objects.requireNonNull(
                    RsaKeyConverters.x509().convert(input),
                    "RSA public key is required");
        }

        if (!privateKey.getModulus().equals(publicKey.getModulus())) {
            throw new IllegalStateException(
                    "JWT public and private keys do not match");
        }

        return NimbusJwtEncoder.withKeyPair(publicKey, privateKey)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.auth.jwt-public-key}") Resource publicKeyResource,
            AuthProperties properties,
            Clock clock) throws IOException {
        RSAPublicKey publicKey;

        try (var input = publicKeyResource.getInputStream()) {
            publicKey = Objects.requireNonNull(
                    RsaKeyConverters.x509().convert(input),
                    "RSA public key is required");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);

        timestamps.setClock(clock);
        timestamps.setAllowEmptyExpiryClaim(false);

        OAuth2TokenValidator<Jwt> claimsValidator = jwt -> {
            Instant now = clock.instant();
            Instant issuedAt = jwt.getIssuedAt();
            Instant expiresAt = jwt.getExpiresAt();

            boolean valid = jwt.getAudience() != null
                    && jwt.getAudience().contains(properties.audience())
                    && issuedAt != null
                    && expiresAt != null
                    && !issuedAt.isAfter(now)
                    && expiresAt.isAfter(issuedAt)
                    && now.isBefore(expiresAt);

            if (valid) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            "invalid_token",
                            ClientMessages.AUTHENTICATION_REQUIRED,
                            null));
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        timestamps,
                        new JwtIssuerValidator(properties.issuer()),
                        claimsValidator));

        return decoder;
    }
}
