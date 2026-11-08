package com.longdnh.onward.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/** The single owner is never demoted or removed; ownership moves only via transfer. */
public final class OwnerProtectedException extends ApiException {

    public OwnerProtectedException() {
        super(HttpStatus.CONFLICT, ApiErrorCodes.OWNER_PROTECTED, ClientMessages.OWNER_PROTECTED);
    }
}
