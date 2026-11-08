package com.longdnh.onward.identity.web.dto;

import java.util.UUID;

public record CurrentUserResponse(UUID userId, String email) {
}
