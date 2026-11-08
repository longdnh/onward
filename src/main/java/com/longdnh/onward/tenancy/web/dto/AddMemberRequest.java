package com.longdnh.onward.tenancy.web.dto;

import java.util.UUID;

import com.longdnh.onward.tenancy.domain.Role;

import jakarta.validation.constraints.NotNull;

/** An unknown role string fails JSON deserialization and becomes 400 before reaching the service. */
public record AddMemberRequest(@NotNull UUID userId, @NotNull Role role) {
}
