package com.longdnh.donot_get_left_behind.common.constants;

/** Stable identifiers for frontend error handling, independent of display-message wording. */
public final class ApiErrorCodes {

    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String MEMBERSHIP_ALREADY_ACTIVE = "MEMBERSHIP_ALREADY_ACTIVE";
    public static final String MEMBERSHIP_NOT_ACTIVE = "MEMBERSHIP_NOT_ACTIVE";
    public static final String OWNER_PROTECTED = "OWNER_PROTECTED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String NOT_ACCEPTABLE = "NOT_ACCEPTABLE";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String REQUEST_FAILED = "REQUEST_FAILED";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String CSRF_INVALID = "CSRF_INVALID";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";

    private ApiErrorCodes() {
    }
}
