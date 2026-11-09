package com.longdnh.onward.identity.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.longdnh.onward.common.constants.ClientMessages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = ClientMessages.TOKEN_REQUIRED)
        @Size(max = 256, message = ClientMessages.INVALID_TOKEN)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String token,
        @NotNull(message = ClientMessages.PASSWORD_REQUIRED)
        @Size(max = 1024, message = ClientMessages.LOGIN_INPUT_TOO_LONG)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[credentials=REDACTED]";
    }
}
