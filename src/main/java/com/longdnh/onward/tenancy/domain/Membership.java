package com.longdnh.donot_get_left_behind.tenancy.domain;

import static com.longdnh.donot_get_left_behind.common.Validation.requireUuidV7;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

@Getter
public final class Membership {

    public enum MembershipStatus {
        ACTIVE,
        REMOVED
    }

    private final UUID id;
    private final UUID organizationId;
    private final UUID userId;
    private final Role role;
    private final MembershipStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Membership(UUID id, UUID organizationId, UUID userId, Role role,
                      MembershipStatus status, Instant createdAt, Instant updatedAt) {
        this.id = requireUuidV7(id, "id");
        this.organizationId = requireUuidV7(organizationId, "organizationId");
        this.userId = requireUuidV7(userId, "userId");
        this.role = Objects.requireNonNull(role, "role is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
    }

    /** OWNER is valid here for organization bootstrap; member-add APIs restrict assignable roles. */
    public static Membership create(UUID id, UUID organizationId, UUID userId, Role role, Instant now) {
        return new Membership(id, organizationId, userId, role, MembershipStatus.ACTIVE, now, now);
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }

    public Membership changeRole(Role newRole, Instant changedAt) {
        if (!isActive()) {
            throw new IllegalStateException("Cannot change the role of a removed membership");
        }
        return changed(newRole, status, changedAt);
    }

    public Membership remove(Instant changedAt) {
        requireChangeTime(changedAt);
        if (!isActive()) {
            return this;
        }
        return changed(role, MembershipStatus.REMOVED, changedAt);
    }

    public Membership reactivate(Role newRole, Instant changedAt) {
        if (isActive()) {
            throw new IllegalStateException("Membership is already active");
        }
        return changed(newRole, MembershipStatus.ACTIVE, changedAt);
    }

    private Membership changed(Role role, MembershipStatus status, Instant changedAt) {
        requireChangeTime(changedAt);
        return new Membership(id, organizationId, userId, role, status, createdAt, changedAt);
    }

    private void requireChangeTime(Instant changedAt) {
        Objects.requireNonNull(changedAt, "changedAt is required");
        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("changedAt must not precede updatedAt");
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Membership membership && id.equals(membership.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
