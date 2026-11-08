package com.longdnh.onward.tenancy.infrastructure.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.longdnh.onward.common.events.UserDisabledEvent;
import com.longdnh.onward.tenancy.application.service.MembershipService;

import lombok.RequiredArgsConstructor;

/**
 * Inbound adapter: turns identity's event into a tenancy use case. A plain @EventListener runs
 * synchronously inside the publisher's transaction, so the membership removal commits or rolls back
 * together with the account disable. (@TransactionalEventListener(AFTER_COMMIT) would run outside it
 * and would need an outbox to survive a crash between commit and listener.)
 */
@Component
@RequiredArgsConstructor
public class UserDisabledListener {

    private final MembershipService membershipService;

    @EventListener
    public void on(UserDisabledEvent event) {
        membershipService.removeAllMembershipsOf(event.userId());
    }
}
