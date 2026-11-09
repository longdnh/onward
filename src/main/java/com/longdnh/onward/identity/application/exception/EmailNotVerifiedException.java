package com.longdnh.onward.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/**
 * Thrown by login only after the password matched, so it reveals nothing to someone without the password.
 * The frontend routes to its "verify your email" page on this code.
 */
public final class EmailNotVerifiedException extends ApiException {

    public EmailNotVerifiedException() {
        super(HttpStatus.FORBIDDEN, ApiErrorCodes.EMAIL_NOT_VERIFIED, ClientMessages.EMAIL_NOT_VERIFIED);
    }
}
