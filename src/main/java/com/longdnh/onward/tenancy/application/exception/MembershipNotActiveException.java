package com.longdnh.onward.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/** Role changes apply to active memberships only; a removed one is reactivated through add-member. */
public final class MembershipNotActiveException extends ApiException {

    public MembershipNotActiveException() {
        super(HttpStatus.CONFLICT, ApiErrorCodes.MEMBERSHIP_NOT_ACTIVE, ClientMessages.MEMBERSHIP_NOT_ACTIVE);
    }
}
