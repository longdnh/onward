package com.longdnh.donot_get_left_behind.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.donot_get_left_behind.common.Exception.ApiException;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

/** Role changes apply to active memberships only; a removed one is reactivated through add-member. */
public final class MembershipNotActiveException extends ApiException {

    public MembershipNotActiveException() {
        super(HttpStatus.CONFLICT, ApiErrorCodes.MEMBERSHIP_NOT_ACTIVE, ClientMessages.MEMBERSHIP_NOT_ACTIVE);
    }
}
