package com.longdnh.onward.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/** The target user does not exist or is not active. Same body as any other 404 so user ids cannot be probed. */
public final class TargetUserUnavailableException extends ApiException {

    public TargetUserUnavailableException() {
        super(HttpStatus.NOT_FOUND, ApiErrorCodes.NOT_FOUND, ClientMessages.RESOURCE_NOT_FOUND);
    }
}
