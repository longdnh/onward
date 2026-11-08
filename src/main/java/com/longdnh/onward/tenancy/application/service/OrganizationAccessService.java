package com.longdnh.donot_get_left_behind.tenancy.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.tenancy.application.exception.InsufficientPermissionException;
import com.longdnh.donot_get_left_behind.tenancy.application.exception.OrganizationNotFoundException;
import com.longdnh.donot_get_left_behind.tenancy.application.port.MembershipRepository;
import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;
import com.longdnh.donot_get_left_behind.tenancy.domain.Permission;

import lombok.RequiredArgsConstructor;

/**
 * Tenant access rules. SecurityFilterChain answers "who is calling"; this class answers
 * "what may the caller do inside this organization". Application services call it, and
 * write paths call it again inside their transaction after locking the organization row.
 */
@Service
@RequiredArgsConstructor
public class OrganizationAccessService {

    private final MembershipRepository membershipRepository;

    /**
     * Returns the caller's active membership in the organization, or throws
     * {@link OrganizationNotFoundException}. A missing organization, a non-member and a
     * REMOVED member are indistinguishable to the caller on purpose.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Membership requireActiveMembership(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(Membership::isActive)
                .orElseThrow(OrganizationNotFoundException::new);
    }

    /**
     * Membership check first (404 for outsiders), then the role matrix (403 for members
     * without the permission). The order matters: a 403 would confirm the organization exists.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Membership requirePermission(UUID organizationId, UUID userId, Permission permission) {
        Membership membership = requireActiveMembership(organizationId, userId);
        if (!membership.getRole().allows(permission)) {
            throw new InsufficientPermissionException();
        }
        return membership;
    }
}
