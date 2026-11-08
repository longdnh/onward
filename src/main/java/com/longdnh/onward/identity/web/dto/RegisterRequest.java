package com.longdnh.donot_get_left_behind.identity.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank(message = ClientMessages.EMAIL_REQUIRED) String email,
        @NotNull(message = ClientMessages.PASSWORD_REQUIRED)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[credentials=REDACTED]";
    }
}
