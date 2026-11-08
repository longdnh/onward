package com.longdnh.donot_get_left_behind.identity.web.dto;

import java.util.UUID;

import com.longdnh.donot_get_left_behind.identity.domain.User;

/** Never includes the password hash. */
public record AdminUserResponse(UUID id, String email, String status, String systemRole) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus().name(),
                user.getSystemRole() == null ? null : user.getSystemRole().name());
    }
}
