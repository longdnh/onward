package com.longdnh.donot_get_left_behind.tenancy.web.dto;

import com.longdnh.donot_get_left_behind.tenancy.domain.Role;

import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}
