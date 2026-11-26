package com.longdnh.onward.identity.application.port;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound port: tells the gateway which access tokens to reject before they expire. The database stays
 * the source of truth for sessions; this is a short-lived copy of the revocations, kept only as long as
 * an access token lives. Implementations must be called after commit (see SessionDenylistListener) and
 * must not throw on an unreachable store: a failed write only means a revoked token is honoured until it
 * expires, which docs/en/microservice-decomposition.md section 7 accepts.
 */
public interface SessionDenylist {

    /** Every access token of this session is rejected from now on. */
    void denySession(UUID sessionId);

    /** Every access token of this user issued at or before {@code revokedAt} is rejected. */
    void denyUserTokensIssuedBefore(UUID userId, Instant revokedAt);
}
