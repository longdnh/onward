package com.longdnh.donot_get_left_behind.tenancy.web.dto;

import java.util.UUID;

import com.longdnh.donot_get_left_behind.tenancy.application.dto.OrganizationSummary;

public record OrganizationSummaryResponse(UUID id, String name, String role) {

    public static OrganizationSummaryResponse from(OrganizationSummary summary) {
        return new OrganizationSummaryResponse(
                summary.organization().getId(),
                summary.organization().getName(),
                summary.role().name());
    }
}
