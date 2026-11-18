package com.longdnh.onward.identity.infrastructure.configuration;

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

import com.longdnh.onward.common.constants.ClientMessages;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    /**
     * The signing key pair as a JWK with a stable key id (the RFC 7638 thumbprint of the public key).
     * The encoder selects the key from this set and writes its {@code kid} into every token header; the
     * JWKS endpoint publishes the public half so other services can verify tokens without a PEM file.
     * Only one key is in the set today; rotation adds the next key here and keeps the old one until its
     * tokens have expired.
     */
    @Bean
    public JWKSet jwkSet(
            @Value("${app.auth.jwt-private-key}") Resource privateKeyResource,
            @Value("${app.auth.jwt-public-key}") Resource publicKeyResource) throws IOException, JOSEException {
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

        RSAKey signingKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build();

        return new JWKSet(signingKey);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSet jwkSet) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(jwkSet));
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSet jwkSet, AuthProperties properties, Clock clock) {
        // Verified from the same in-memory set the JWKS endpoint serves, selected by kid like a remote
        // consumer would, so Account proves the published keys are the ones that sign its tokens.
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSource(new ImmutableJWKSet<>(jwkSet))
                .jwsAlgorithm(SignatureAlgorithm.RS256)
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
