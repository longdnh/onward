package com.longdnh.onward.identity.infrastructure.denylist;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.longdnh.onward.identity.application.event.SessionRevoked;
import com.longdnh.onward.identity.application.event.UserTokensRevoked;
import com.longdnh.onward.identity.application.port.SessionDenylist;

import lombok.RequiredArgsConstructor;

/**
 * Copies a committed revocation to the denylist. Deliberately synchronous, unlike the mail listener: the
 * write is one Redis command and the point of the denylist is that the revocation is visible to the
 * gateway before the client gets its response. Runs only after commit, so a rolled-back revocation never
 * blocks a live session. Failures are swallowed inside the adapter (fail-open).
 */
@Component
@RequiredArgsConstructor
public class SessionDenylistListener {

    private final SessionDenylist denylist;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(SessionRevoked event) {
        denylist.denySession(event.sessionId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserTokensRevoked event) {
        denylist.denyUserTokensIssuedBefore(event.userId(), event.revokedAt());
    }
}
