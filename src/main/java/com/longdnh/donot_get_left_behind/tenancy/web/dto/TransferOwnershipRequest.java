package com.longdnh.donot_get_left_behind.tenancy.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** The membership (not the user) that receives ownership; it must belong to the organization in the route. */
public record TransferOwnershipRequest(@NotNull UUID membershipId) {
}
