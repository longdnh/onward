package com.longdnh.donot_get_left_behind.identity.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.donot_get_left_behind.common.Exception.ApiException;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

/** Disabling would leave at least one organization without an owner; the user must transfer ownership first. */
public final class OwnershipMustBeTransferredException extends ApiException {

    public OwnershipMustBeTransferredException() {
        super(HttpStatus.CONFLICT, ApiErrorCodes.OWNERSHIP_MUST_BE_TRANSFERRED, ClientMessages.OWNERSHIP_MUST_BE_TRANSFERRED);
    }
}
