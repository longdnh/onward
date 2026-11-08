package com.longdnh.donot_get_left_behind.tenancy.web.dto;

import java.util.UUID;

import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;

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
