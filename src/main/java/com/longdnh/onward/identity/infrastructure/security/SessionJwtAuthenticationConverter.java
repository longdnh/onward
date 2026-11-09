package com.longdnh.onward.identity.infrastructure.security;

import static com.longdnh.onward.common.Validation.requireUuidV7;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.longdnh.onward.common.constants.ClientMessages;
import com.longdnh.onward.identity.application.port.AuthSessionRepository;
import com.longdnh.onward.identity.application.port.UserRepository;
import com.longdnh.onward.identity.infrastructure.configuration.AuthProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SessionJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AuthSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AuthProperties properties;
    private final Clock clock;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = readUuidClaim(jwt, "sub");
        UUID sessionId = readUuidClaim(jwt, "sid");

        var session = sessionRepository.findById(sessionId)
                .orElseThrow(
                        SessionJwtAuthenticationConverter::invalidAuthentication);

        Instant now = clock.instant();

        if (!session.getUserId().equals(userId)
                || !session.isActiveAt(now, properties.idleTimeout())) {
            throw invalidAuthentication();
        }

        var user = userRepository.findById(userId)
                .orElseThrow(
                        SessionJwtAuthenticationConverter::invalidAuthentication);

        if (!user.canAuthenticate()) {
            throw invalidAuthentication();
        }

        boolean recorded = sessionRepository.recordActivityIfActive(
                sessionId,
                userId,
                clock.instant(),
                properties.idleTimeout());

        if (!recorded) {
            throw invalidAuthentication();
        }

        // System role is read from the database on every request (not baked into the JWT), so revoking
        // it takes effect immediately. Tenant roles are NOT authorities: they depend on the route's organization.
        List<GrantedAuthority> authorities = user.isSystemAdmin()
                ? List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))
                : List.of();

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                userId.toString());
    }

    private static UUID readUuidClaim(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);

        if (!(value instanceof String text)) {
            throw invalidAuthentication();
        }

        try {
            UUID id = UUID.fromString(text);

            if (!id.toString().equals(text)) {
                throw invalidAuthentication();
            }

            return requireUuidV7(id, name);
        } catch (IllegalArgumentException exception) {
            throw invalidAuthentication();
        }
    }

    private static BadCredentialsException invalidAuthentication() {
        return new BadCredentialsException(
                ClientMessages.AUTHENTICATION_REQUIRED);
    }
}
