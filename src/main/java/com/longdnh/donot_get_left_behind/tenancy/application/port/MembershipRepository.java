package com.longdnh.donot_get_left_behind.tenancy.application.port;

import java.util.Optional;
import java.util.UUID;

import com.longdnh.donot_get_left_behind.common.pagination.PageRequest;
import com.longdnh.donot_get_left_behind.common.pagination.PageResult;
import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;

public interface MembershipRepository {

    /** Inserts a new membership. Must be called inside the caller's transaction. */
    Membership create(Membership membership);

    /**
     * Finds the membership of a user in an organization regardless of status.
     * Callers decide what a REMOVED membership means (deny access, or reactivate on add-member).
     * At most one row exists per (organization, user). Must be called inside the caller's transaction.
     */
    Optional<Membership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    /** Writes the new state of an existing membership. Must be called inside the caller's transaction. */
    Membership update(Membership membership);

    /**
     * All memberships of one organization, any status, ordered by membership id.
     * Always scoped by organization: a membership is never listed outside its tenant.
     * Must be called inside the caller's transaction.
     */
    PageResult<Membership> findByOrganizationId(UUID organizationId, PageRequest page);
}
