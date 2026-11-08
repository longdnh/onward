package com.longdnh.onward.common.events;

import java.util.UUID;

/**
 * Published by identity inside the transaction that disables the account. Lives in common so
 * that tenancy can listen without importing identity. In this single-database monolith the
 * listener runs synchronously in the same transaction, so publisher and listener commit or roll
 * back together. If the consumer ever moves out of this process (another service, a broker),
 * this becomes an outbox row written in the same transaction and relayed afterwards.
 */
public record UserDisabledEvent(UUID userId) {
}
