package com.longdnh.onward.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/** Adding a user who is already an active member; adding never changes an existing role. */
public final class MembershipAlreadyActiveException extends ApiException {

    public MembershipAlreadyActiveException() {
        super(HttpStatus.CONFLICT, ApiErrorCodes.MEMBERSHIP_ALREADY_ACTIVE, ClientMessages.MEMBERSHIP_ALREADY_ACTIVE);
    }
}
