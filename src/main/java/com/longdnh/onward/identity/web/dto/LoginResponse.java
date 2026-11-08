package com.longdnh.donot_get_left_behind.identity.web.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    @Override
    public String toString() {
        return "LoginResponse[accessToken=REDACTED]";
    }
}
