package com.longdnh.onward.tenancy.web.dto;

import com.longdnh.onward.tenancy.domain.Organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @NotBlank @Size(max = Organization.MAX_NAME_LENGTH) String name) {
}
