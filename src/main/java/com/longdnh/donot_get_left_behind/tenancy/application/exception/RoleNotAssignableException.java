package com.longdnh.donot_get_left_behind.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.donot_get_left_behind.common.Exception.ApiException;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

/** OWNER cannot be granted through add-member; use the role-change endpoint. */
public final class RoleNotAssignableException extends ApiException {

    public RoleNotAssignableException() {
        super(HttpStatus.BAD_REQUEST, ApiErrorCodes.INVALID_REQUEST, ClientMessages.ROLE_NOT_ASSIGNABLE);
    }
}
