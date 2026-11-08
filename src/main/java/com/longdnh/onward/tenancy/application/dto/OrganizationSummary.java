package com.longdnh.onward.tenancy.application.dto;

import com.longdnh.onward.tenancy.domain.Organization;
import com.longdnh.onward.tenancy.domain.Role;

/** One row of "the caller's organizations": the organization plus the caller's role in it. */
public record OrganizationSummary(Organization organization, Role role) {
}
