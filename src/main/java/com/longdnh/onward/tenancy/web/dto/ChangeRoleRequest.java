package com.longdnh.onward.tenancy.web.dto;

import com.longdnh.onward.tenancy.domain.Role;

import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}
