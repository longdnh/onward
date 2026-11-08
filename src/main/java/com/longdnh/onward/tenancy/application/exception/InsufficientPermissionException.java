package com.longdnh.onward.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/** The caller is an active member of the organization but their role does not allow the action. */
public final class InsufficientPermissionException extends ApiException {

    public InsufficientPermissionException() {
        super(HttpStatus.FORBIDDEN, ApiErrorCodes.FORBIDDEN, ClientMessages.ACCESS_DENIED);
    }
}
