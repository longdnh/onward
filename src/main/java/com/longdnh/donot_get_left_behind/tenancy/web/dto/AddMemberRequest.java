package com.longdnh.donot_get_left_behind.tenancy.web.dto;

import java.util.UUID;

import com.longdnh.donot_get_left_behind.tenancy.domain.Role;

import jakarta.validation.constraints.NotNull;

/** An unknown role string fails JSON deserialization and becomes 400 before reaching the service. */
public record AddMemberRequest(@NotNull UUID userId, @NotNull Role role) {
}
