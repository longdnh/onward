package com.longdnh.onward.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/** One response for unknown, expired, consumed and wrong-purpose tokens: the client learns nothing more. */
public final class InvalidTokenException extends ApiException {

    public InvalidTokenException() {
        super(HttpStatus.BAD_REQUEST, ApiErrorCodes.INVALID_TOKEN, ClientMessages.INVALID_TOKEN);
    }
}
