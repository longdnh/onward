package com.longdnh.donot_get_left_behind.tenancy.application.dto;

import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;
import com.longdnh.donot_get_left_behind.tenancy.domain.Role;

/** One row of "the caller's organizations": the organization plus the caller's role in it. */
public record OrganizationSummary(Organization organization, Role role) {
}
