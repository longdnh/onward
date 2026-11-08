package com.longdnh.onward.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/** Admin API: no such user. */
public final class UserNotFoundException extends ApiException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, ApiErrorCodes.NOT_FOUND, ClientMessages.RESOURCE_NOT_FOUND);
    }
}
