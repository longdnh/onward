package com.longdnh.donot_get_left_behind.tenancy.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.common.id.IdGenerator;
import com.longdnh.donot_get_left_behind.common.pagination.PageRequest;
import com.longdnh.donot_get_left_behind.common.pagination.PageResult;
import com.longdnh.donot_get_left_behind.tenancy.application.dto.CreateOrganizationResult;
import com.longdnh.donot_get_left_behind.tenancy.application.dto.OrganizationSummary;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.OrganizationNotFoundException;
import com.longdnh.donot_get_left_behind.tenancy.application.port.MembershipRepository;
import com.longdnh.donot_get_left_behind.tenancy.application.port.OrganizationRepository;
import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;
import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;
import com.longdnh.donot_get_left_behind.tenancy.domain.Permission;
import com.longdnh.donot_get_left_behind.tenancy.domain.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationAccessService accessService;
    private final IdGenerator idGenerator;
    private final Clock clock;

    /** The organization and its first owner membership are committed together or not at all. */
    @Transactional
    public CreateOrganizationResult create(UUID ownerUserId, String name) {
        Instant now = clock.instant();

        Organization organization = Organization.create(idGenerator.generate(), name, now);
        Membership ownerMembership = Membership.create(
                idGenerator.generate(),
                organization.getId(),
                ownerUserId,
                Role.OWNER,
                now);

        // The membership references the organization, so the organization is inserted first.
        return new CreateOrganizationResult(
                organizationRepository.create(organization),
                membershipRepository.create(ownerMembership));
    }

    /** Any active member may read the profile; everyone else gets the same 404. */
    @Transactional(readOnly = true)
    public Organization getProfile(UUID organizationId, UUID callerUserId) {
        accessService.requirePermission(organizationId, callerUserId, Permission.READ_ORGANIZATION);
        // The FK guarantees the organization exists once a membership does; the orElseThrow is defensive.
        return organizationRepository.findById(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
    }

    /** No access check needed: the query is already scoped to the caller's own memberships. */
    @Transactional(readOnly = true)
    public PageResult<OrganizationSummary> listForUser(UUID callerUserId, PageRequest page) {
        return organizationRepository.findActiveByMemberUserId(callerUserId, page);
    }

    /**
     * No organization-row lock here: the only invariant is on the name itself, so a caller
     * who loses their role while this runs produces the same state as if they had been
     * a few milliseconds earlier. Membership mutations are the ones that lock.
     */
    @Transactional
    public Organization rename(UUID organizationId, UUID callerUserId, String name) {
        accessService.requirePermission(organizationId, callerUserId, Permission.UPDATE_ORGANIZATION);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
        return organizationRepository.update(organization.rename(name, clock.instant()));
    }
}
