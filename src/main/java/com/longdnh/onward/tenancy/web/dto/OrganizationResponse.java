package com.longdnh.donot_get_left_behind.tenancy.web.dto;

import java.util.UUID;

import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;

public record OrganizationResponse(UUID id, String name) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(organization.getId(), organization.getName());
    }
}
