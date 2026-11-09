package com.longdnh.onward.identity.web.dto;

import com.longdnh.onward.common.constants.ClientMessages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of forgot-password and resend-verification. */
public record EmailRequest(
        @NotBlank(message = ClientMessages.EMAIL_REQUIRED)
        @Size(max = 1024, message = ClientMessages.LOGIN_INPUT_TOO_LONG) String email) {
}
