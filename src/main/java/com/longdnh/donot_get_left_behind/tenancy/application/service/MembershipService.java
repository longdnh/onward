package com.longdnh.donot_get_left_behind.tenancy.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.common.id.IdGenerator;
import com.longdnh.donot_get_left_behind.common.pagination.PageRequest;
import com.longdnh.donot_get_left_behind.common.pagination.PageResult;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.InsufficientPermissionException;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.MembershipAlreadyActiveException;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.MembershipNotActiveException;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.OrganizationNotFoundException;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.OwnerProtectedException;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.RoleNotAssignableException;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.TargetUserUnavailableException;
import com.longdnh.donot_get_left_behind.tenancy.application.dto.OwnershipTransferResult;
import com.longdnh.donot_get_left_behind.tenancy.application.port.MembershipRepository;
import com.longdnh.donot_get_left_behind.tenancy.application.port.OrganizationRepository;
import com.longdnh.donot_get_left_behind.tenancy.application.port.UserAccountLookup;
import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;
import com.longdnh.donot_get_left_behind.tenancy.domain.Permission;
import com.longdnh.donot_get_left_behind.tenancy.domain.Role;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserAccountLookup userAccountLookup;
    private final OrganizationAccessService accessService;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Membership getOwn(UUID organizationId, UUID callerUserId) {
        return accessService.requirePermission(organizationId, callerUserId, Permission.READ_OWN_MEMBERSHIP);
    }

    @Transactional(readOnly = true)
    public PageResult<Membership> list(UUID organizationId, UUID callerUserId, PageRequest page) {
        accessService.requirePermission(organizationId, callerUserId, Permission.LIST_MEMBERSHIPS);
        return membershipRepository.findByOrganizationId(organizationId, page);
    }

    /**
     * Mutation path. The organization row is locked FIRST, and every check below reads state
     * after the lock, so two concurrent membership changes in the same organization run one
     * after the other and the second one sees the first one's result.
     */
    @Transactional
    public Membership add(UUID organizationId, UUID callerUserId, UUID targetUserId, Role role) {
        // Pure input validation: no need to hold the lock for a request that can never succeed.
        if (role == Role.OWNER) {
            throw new RoleNotAssignableException();
        }

        // Missing organization and non-member look the same (404) — the lock lookup doubles as existence check.
        organizationRepository.findByIdForUpdate(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);

        Membership actor = accessService.requirePermission(organizationId, callerUserId, Permission.ADD_MEMBER);
        if (role == Role.ADMIN && !actor.getRole().allows(Permission.ADD_ADMIN)) {
            throw new InsufficientPermissionException();
        }

        if (!userAccountLookup.isActiveUser(targetUserId)) {
            throw new TargetUserUnavailableException();
        }

        Instant now = clock.instant();
        Optional<Membership> existing = membershipRepository.findByOrganizationIdAndUserId(organizationId, targetUserId);
        if (existing.isPresent()) {
            Membership current = existing.get();
            if (current.isActive()) {
                throw new MembershipAlreadyActiveException();
            }
            // One (organization, user) row for life: a removed member comes back by reactivating it.
            return membershipRepository.update(current.reactivate(role, now));
        }
        return membershipRepository.create(
                Membership.create(idGenerator.generate(), organizationId, targetUserId, role, now));
    }

    /** OWNER only, between ADMIN and MEMBER. The owner's own membership is off limits here. */
    @Transactional
    public Membership changeRole(UUID organizationId, UUID callerUserId, UUID membershipId, Role newRole) {
        if (newRole == Role.OWNER) {
            throw new RoleNotAssignableException();
        }
        organizationRepository.findByIdForUpdate(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
        accessService.requirePermission(organizationId, callerUserId, Permission.CHANGE_ROLE);

        Membership target = membershipRepository.findByOrganizationIdAndId(organizationId, membershipId)
                .orElseThrow(OrganizationNotFoundException::new);
        if (!target.isActive()) {
            throw new MembershipNotActiveException();
        }
        if (target.getRole() == Role.OWNER) {
            throw new OwnerProtectedException();
        }
        if (target.getRole() == newRole) {
            return target; // idempotent: nothing to write
        }
        return membershipRepository.update(target.changeRole(newRole, clock.instant()));
    }

    /**
     * Marks the membership REMOVED. Idempotent: removing an already removed membership is a no-op 204,
     * so a retried DELETE never fails. Admins may remove MEMBERs only; the owner may remove anyone
     * except themselves — ownership has to be transferred first.
     */
    @Transactional
    public void remove(UUID organizationId, UUID callerUserId, UUID membershipId) {
        organizationRepository.findByIdForUpdate(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
        Membership actor = accessService.requirePermission(organizationId, callerUserId, Permission.REMOVE_MEMBER);

        Membership target = membershipRepository.findByOrganizationIdAndId(organizationId, membershipId)
                .orElseThrow(OrganizationNotFoundException::new);
        if (!target.isActive()) {
            return;
        }
        // Target-specific rule the permission matrix cannot express: it depends on the target's role,
        // which is read here, after the lock — so a member promoted a moment ago is already protected.
        if (target.getRole() == Role.OWNER) {
            throw new OwnerProtectedException();
        }
        if (target.getRole() == Role.ADMIN && !actor.getRole().allows(Permission.REMOVE_ADMIN)) {
            throw new InsufficientPermissionException();
        }
        membershipRepository.update(target.remove(clock.instant()));
    }

    /**
     * Swaps roles between the current owner (the caller) and an active member, in one transaction
     * under the organization lock. The order of the two writes matters: the old owner steps down
     * first, otherwise the unique index on active owners rejects the second OWNER row.
     * The application keeps "at least one owner"; the database keeps "at most one".
     */
    @Transactional
    public OwnershipTransferResult transferOwnership(UUID organizationId, UUID callerUserId, UUID targetMembershipId) {
        organizationRepository.findByIdForUpdate(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
        Membership owner = accessService.requirePermission(organizationId, callerUserId, Permission.TRANSFER_OWNERSHIP);

        Membership target = membershipRepository.findByOrganizationIdAndId(organizationId, targetMembershipId)
                .orElseThrow(OrganizationNotFoundException::new);
        if (target.getId().equals(owner.getId())) {
            return new OwnershipTransferResult(owner, owner); // transferring to oneself: nothing to do
        }
        if (!target.isActive()) {
            throw new MembershipNotActiveException();
        }
        // A disabled account must never end up holding the only owner seat.
        if (!userAccountLookup.isActiveUser(target.getUserId())) {
            throw new TargetUserUnavailableException();
        }

        Instant now = clock.instant();
        Membership previousOwner = membershipRepository.update(owner.changeRole(Role.ADMIN, now));
        Membership newOwner = membershipRepository.update(target.changeRole(Role.OWNER, now));
        return new OwnershipTransferResult(newOwner, previousOwner);
    }
}
