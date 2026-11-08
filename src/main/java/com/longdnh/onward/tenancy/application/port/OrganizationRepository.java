package com.longdnh.onward.tenancy.application.port;

import java.util.Optional;
import java.util.UUID;

import com.longdnh.onward.common.pagination.PageRequest;
import com.longdnh.onward.common.pagination.PageResult;
import com.longdnh.onward.tenancy.application.dto.OrganizationSummary;
import com.longdnh.onward.tenancy.domain.Organization;

public interface OrganizationRepository {

    /** Inserts a new organization. Must be called inside the caller's transaction. */
    Organization create(Organization organization);

    /** Must be called inside the caller's transaction. */
    Optional<Organization> findById(UUID organizationId);

    /**
     * Loads the organization with a row lock (SELECT ... FOR UPDATE) held until the caller's
     * transaction ends. Every membership mutation of one organization goes through this lock,
     * so their checks and writes are serialized per tenant. Must be called inside the caller's transaction.
     */
    Optional<Organization> findByIdForUpdate(UUID organizationId);

    /** Writes the new state of an existing organization. Must be called inside the caller's transaction. */
    Organization update(Organization organization);

    /**
     * Organizations in which the user holds an ACTIVE membership, ordered by organization id
     * (UUIDv7, so creation order and a total order). Must be called inside the caller's transaction.
     */
    PageResult<OrganizationSummary> findActiveByMemberUserId(UUID userId, PageRequest page);
}
