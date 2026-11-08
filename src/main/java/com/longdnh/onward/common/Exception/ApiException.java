package com.longdnh.donot_get_left_behind.common.Exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors that modules want to surface to the client as {code, message}.
 * The message is sent to the client as-is, so subclasses must pass a fixed, safe message.
 * common only knows this class; it never imports a module's exception types.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
