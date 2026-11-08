package com.longdnh.onward.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/** OWNER cannot be granted through add-member; use the role-change endpoint. */
public final class RoleNotAssignableException extends ApiException {

    public RoleNotAssignableException() {
        super(HttpStatus.BAD_REQUEST, ApiErrorCodes.INVALID_REQUEST, ClientMessages.ROLE_NOT_ASSIGNABLE);
    }
}
