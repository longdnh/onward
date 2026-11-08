package com.longdnh.donot_get_left_behind.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.donot_get_left_behind.common.Exception.ApiException;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

/** Admin API: no such user. */
public final class UserNotFoundException extends ApiException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, ApiErrorCodes.NOT_FOUND, ClientMessages.RESOURCE_NOT_FOUND);
    }
}
