package com.longdnh.onward.identity.application.dto;

import java.time.Instant;

public record RefreshResult(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {

    @Override
    public String toString() {
        return "RefreshResult{tokens=[REDACTED]}";
    }
}