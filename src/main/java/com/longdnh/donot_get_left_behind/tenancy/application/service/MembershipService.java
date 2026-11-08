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
import com.longdnh.donot_get_left_behind.tenancy.application.exception.OrganizationNotFoundException;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.RoleNotAssignableException;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.TargetUserUnavailableException;
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
}
