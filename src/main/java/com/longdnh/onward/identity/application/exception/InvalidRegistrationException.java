package com.longdnh.onward.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;

/** Invalid client input, distinct from a programming error or invalid persisted state. */
public final class InvalidRegistrationException extends ApiException {

    public InvalidRegistrationException(String message) {
        super(HttpStatus.BAD_REQUEST, ApiErrorCodes.INVALID_REQUEST, message);
    }
}
