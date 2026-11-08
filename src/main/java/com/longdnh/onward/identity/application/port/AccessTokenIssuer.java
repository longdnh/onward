package com.longdnh.donot_get_left_behind.identity.application.port;

import java.time.Instant;
import java.util.UUID;

public interface AccessTokenIssuer {
    String issue(
            UUID userId,
            UUID sessionId,
            Instant issuedAt,
            Instant expiresAt);
}
