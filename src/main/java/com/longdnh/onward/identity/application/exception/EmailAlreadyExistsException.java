package com.longdnh.onward.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

public final class EmailAlreadyExistsException extends ApiException {

    public EmailAlreadyExistsException() {
        super(HttpStatus.CONFLICT, ApiErrorCodes.EMAIL_ALREADY_EXISTS, ClientMessages.EMAIL_ALREADY_EXISTS);
    }
}
