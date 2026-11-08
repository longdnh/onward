
package com.longdnh.donot_get_left_behind.identity.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = ClientMessages.EMAIL_REQUIRED)
    @Size(max = 1024,
          message = ClientMessages.LOGIN_INPUT_TOO_LONG) String email,

    @NotNull(message = ClientMessages.PASSWORD_REQUIRED)
    @Size(max = 1024, message = ClientMessages.LOGIN_INPUT_TOO_LONG)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {

    @Override
    public String toString() {
        return "LoginRequest[credentials=REDACTED]";
    }
}