package com.longdnh.donot_get_left_behind.tenancy.web.dto;

import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = Organization.MAX_NAME_LENGTH) String name) {
}
