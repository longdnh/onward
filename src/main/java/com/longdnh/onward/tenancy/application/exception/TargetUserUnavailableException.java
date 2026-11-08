package com.longdnh.donot_get_left_behind.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.donot_get_left_behind.common.Exception.ApiException;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

/** The target user does not exist or is not active. Same body as any other 404 so user ids cannot be probed. */
public final class TargetUserUnavailableException extends ApiException {

    public TargetUserUnavailableException() {
        super(HttpStatus.NOT_FOUND, ApiErrorCodes.NOT_FOUND, ClientMessages.RESOURCE_NOT_FOUND);
    }
}
