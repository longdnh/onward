package com.longdnh.onward.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/** An admin cannot disable their own account through the admin API; self-disable exists for that. */
public final class SelfActionNotAllowedException extends ApiException {

    public SelfActionNotAllowedException() {
        super(HttpStatus.CONFLICT, ApiErrorCodes.SELF_ACTION_NOT_ALLOWED, ClientMessages.SELF_ACTION_NOT_ALLOWED);
    }
}
