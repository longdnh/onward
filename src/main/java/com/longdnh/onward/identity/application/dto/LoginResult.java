package com.longdnh.donot_get_left_behind.identity.application.dto;

import java.time.Instant;

public record LoginResult(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {

    @Override
    public String toString() {
        return "LoginResult{tokens=[REDACTED]}";
    }
}