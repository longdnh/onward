package com.longdnh.onward.identity.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.longdnh.onward.common.constants.ClientMessages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotBlank(message = ClientMessages.TOKEN_REQUIRED)
        @Size(max = 256, message = ClientMessages.INVALID_TOKEN)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String token) {

    @Override
    public String toString() {
        return "VerifyEmailRequest[token=REDACTED]";
    }
}
