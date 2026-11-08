package com.longdnh.donot_get_left_behind.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.donot_get_left_behind.common.Exception.ApiException;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

public final class EmailAlreadyExistsException extends ApiException {

    public EmailAlreadyExistsException() {
        super(HttpStatus.CONFLICT, ApiErrorCodes.EMAIL_ALREADY_EXISTS, ClientMessages.EMAIL_ALREADY_EXISTS);
    }
}
