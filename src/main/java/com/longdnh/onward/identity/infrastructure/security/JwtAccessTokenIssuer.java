package com.longdnh.donot_get_left_behind.identity.infrastructure.security;

import static com.longdnh.donot_get_left_behind.common.Validation.requireUuidV7;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.longdnh.donot_get_left_behind.identity.application.port.AccessTokenIssuer;
import com.longdnh.donot_get_left_behind.identity.infrastructure.configuration.AuthProperties;

@Component 
public final class JwtAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;

    public JwtAccessTokenIssuer(JwtEncoder jwtEncoder, AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public String issue(UUID userId, UUID sessionId, Instant issuedAt, Instant expiresAt) {
        requireUuidV7(userId, "userId");
        requireUuidV7(sessionId, "sessionId");
        Objects.requireNonNull(issuedAt, "issuedAt is required");
        Objects.requireNonNull(expiresAt, "expiresAt is required");

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt");
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.issuer())
                .audience(List.of(authProperties.audience()))
                .subject(userId.toString())
                .claim("sid", sessionId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    }

}
