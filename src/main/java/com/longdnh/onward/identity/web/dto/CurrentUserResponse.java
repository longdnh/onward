package com.longdnh.donot_get_left_behind.identity.web.dto;

import java.util.UUID;

public record CurrentUserResponse(UUID userId, String email) {
}
