package com.longdnh.donot_get_left_behind.identity.web.dto;

public record CsrfResponse(String headerName, String token) {

    @Override
    public String toString() {
        return "CsrfResponse[token=REDACTED]";
    }
}
