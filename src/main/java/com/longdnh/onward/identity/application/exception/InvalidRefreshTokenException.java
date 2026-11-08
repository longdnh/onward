package com.longdnh.onward.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

public final class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, ApiErrorCodes.INVALID_REFRESH_TOKEN, ClientMessages.INVALID_REFRESH_TOKEN);
    }
}
