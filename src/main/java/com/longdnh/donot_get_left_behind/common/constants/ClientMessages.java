package com.longdnh.donot_get_left_behind.common.constants;

/** Client-facing text shared by input validation, application exceptions, and HTTP responses. */
public final class ClientMessages {

    public static final String EMAIL_REQUIRED = "email is required";
    public static final String EMAIL_TOO_LONG = "email must not exceed %d characters";
    public static final String EMAIL_INVALID = "email must be a valid email address";
    public static final String EMAIL_ALREADY_EXISTS = "An account with this email already exists";
    public static final String MEMBERSHIP_ALREADY_ACTIVE = "The user is already an active member of this organization";
    public static final String ROLE_NOT_ASSIGNABLE = "Only ADMIN or MEMBER can be assigned; ownership changes through an ownership transfer";
    public static final String MEMBERSHIP_NOT_ACTIVE = "The membership has been removed; add the user again to reactivate it";
    public static final String OWNER_PROTECTED = "The owner's membership can only change through an ownership transfer";

    public static final String PASSWORD_REQUIRED = "password is required";
    public static final String PASSWORD_LENGTH_OUT_OF_RANGE = "password must contain between %d and %d characters";
    public static final String PASSWORD_TOO_COMMON = "password is too common";

    public static final String INVALID_REGISTRATION_REQUEST =
            "Provide a JSON object with valid email and password fields only";
    public static final String INVALID_REQUEST = "The request is invalid";
    public static final String RESOURCE_NOT_FOUND = "The requested resource was not found";
    public static final String METHOD_NOT_ALLOWED = "The HTTP method is not supported for this endpoint";
    public static final String JSON_RESPONSE_REQUIRED = "This endpoint produces application/json";
    public static final String JSON_CONTENT_TYPE_REQUIRED = "Use Content-Type: application/json";
    public static final String REQUEST_FAILED = "Unable to process the request";

    public static final String AUTHENTICATION_REQUIRED = "Authentication is required";
    public static final String CSRF_TOKEN_REQUIRED = "A valid CSRF token is required";
    public static final String ACCESS_DENIED = "Access is denied";
    public static final String REGISTRATION_RATE_LIMITED = "Too many registration attempts. Try again later";
    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    public static final String LOGIN_INPUT_TOO_LONG = "Login input is too long";
    public static final String INVALID_REFRESH_TOKEN = "Unable to refresh authentication. Please sign in again";

    private ClientMessages() {
    }
}
