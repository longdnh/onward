package com.longdnh.donot_get_left_behind.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.donot_get_left_behind.common.Exception.ApiException;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

public final class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, ApiErrorCodes.INVALID_REFRESH_TOKEN, ClientMessages.INVALID_REFRESH_TOKEN);
    }
}
