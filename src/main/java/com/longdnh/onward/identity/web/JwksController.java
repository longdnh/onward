package com.longdnh.onward.identity.web;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.longdnh.onward.common.constants.ApiPaths;
import com.nimbusds.jose.jwk.JWKSet;

import lombok.RequiredArgsConstructor;

/**
 * Publishes the public signing keys (RFC 7517) so other services verify access tokens on their own.
 * Public and unauthenticated by design: the set holds no secret. Consumers cache it, so a rotated key
 * must be published here before it signs anything.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private static final Duration MAX_AGE = Duration.ofMinutes(5);

    private final JWKSet jwkSet;

    @GetMapping(path = ApiPaths.WellKnown.JWKS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwks() {
        // toJSONObject(true) keeps only the public parameters; the private key never leaves the process.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(MAX_AGE).cachePublic())
                .body(jwkSet.toJSONObject(true));
    }
}
