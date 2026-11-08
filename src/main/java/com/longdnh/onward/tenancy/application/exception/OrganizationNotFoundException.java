package com.longdnh.onward.tenancy.application.exception;

import org.springframework.http.HttpStatus;

import com.longdnh.onward.common.Exception.ApiException;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

/**
 * Thrown both when the organization does not exist and when the caller is not an active member.
 * Same status, code and message in both cases so a non-member cannot probe which ids exist.
 */
public final class OrganizationNotFoundException extends ApiException {

    public OrganizationNotFoundException() {
        super(HttpStatus.NOT_FOUND, ApiErrorCodes.NOT_FOUND, ClientMessages.RESOURCE_NOT_FOUND);
    }
}
