package com.longdnh.donot_get_left_behind.tenancy.application.dto;

import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;
import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;

public record CreateOrganizationResult(Organization organization, Membership ownerMembership) {
}
