package com.longdnh.onward.tenancy.application.dto;

import com.longdnh.onward.tenancy.domain.Membership;
import com.longdnh.onward.tenancy.domain.Organization;

public record CreateOrganizationResult(Organization organization, Membership ownerMembership) {
}
