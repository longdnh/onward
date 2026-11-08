package com.longdnh.onward.tenancy.web.dto;

import java.util.UUID;

import com.longdnh.onward.tenancy.domain.Membership;

public record MembershipResponse(UUID id, UUID organizationId, UUID userId, String role, String status) {

    public static MembershipResponse from(Membership membership) {
        return new MembershipResponse(
                membership.getId(),
                membership.getOrganizationId(),
                membership.getUserId(),
                membership.getRole().name(),
                membership.getStatus().name());
    }
}
