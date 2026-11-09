package com.longdnh.onward.identity.web.dto;

import java.util.UUID;

import com.longdnh.onward.identity.domain.User;

/** Never includes the password hash. */
public record AdminUserResponse(UUID id, String email, String status, String systemRole, boolean emailVerified) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus().name(),
                user.getSystemRole() == null ? null : user.getSystemRole().name(),
                user.isEmailVerified());
    }
}
