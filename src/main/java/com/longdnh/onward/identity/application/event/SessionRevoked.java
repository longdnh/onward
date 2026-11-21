package com.longdnh.onward.identity.application.event;

import java.util.UUID;

/**
 * Published inside the transaction that revokes one session (logout, refresh-token reuse). The denylist
 * listener runs AFTER_COMMIT so the gateway rejects the session's access tokens only once the revocation
 * is durable; a rolled-back revocation never blocks a live session.
 */
public record SessionRevoked(UUID sessionId) {
}
