package com.longdnh.onward.identity.web.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    @Override
    public String toString() {
        return "LoginResponse[accessToken=REDACTED]";
    }
}
