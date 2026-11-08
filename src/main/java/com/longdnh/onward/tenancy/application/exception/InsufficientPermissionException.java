package com.longdnh.donot_get_left_behind.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.donot_get_left_behind.common.Exception.ApiException;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

/** The caller is an active member of the organization but their role does not allow the action. */
public final class InsufficientPermissionException extends ApiException {

    public InsufficientPermissionException() {
        super(HttpStatus.FORBIDDEN, ApiErrorCodes.FORBIDDEN, ClientMessages.ACCESS_DENIED);
    }
}
