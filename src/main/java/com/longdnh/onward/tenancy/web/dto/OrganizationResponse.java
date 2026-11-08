package com.longdnh.onward.tenancy.web.dto;

import java.util.UUID;

import com.longdnh.onward.tenancy.domain.Organization;

public record OrganizationResponse(UUID id, String name) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(organization.getId(), organization.getName());
    }
}
