package com.longdnh.donot_get_left_behind.common.Exception;

import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Every module-specific error funnels through here; common never imports module exception types. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> apiException(ApiException exception) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpectedFailure(Exception exception) {
        // Exception messages may contain SQL details or credentials; log only the type
        // here.
        logger.error("Unhandled request exception type: " + exception.getClass().getName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCodes.INTERNAL_ERROR, ClientMessages.REQUEST_FAILED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> unauthenticated() {
        return error(HttpStatus.UNAUTHORIZED, ApiErrorCodes.UNAUTHENTICATED, ClientMessages.AUTHENTICATION_REQUIRED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException exception) {
        if (exception instanceof CsrfException) {
            return error(HttpStatus.FORBIDDEN, ApiErrorCodes.CSRF_INVALID, ClientMessages.CSRF_TOKEN_REQUIRED);
        }
        return error(HttpStatus.FORBIDDEN, ApiErrorCodes.FORBIDDEN, ClientMessages.ACCESS_DENIED);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Framework exceptions may contain rejected passwords or parser excerpts: use
        // fixed messages.
        ApiError error = switch (status.value()) {
            case 400 -> new ApiError(ApiErrorCodes.INVALID_REQUEST, ClientMessages.INVALID_REQUEST);
            case 404 -> new ApiError(ApiErrorCodes.NOT_FOUND, ClientMessages.RESOURCE_NOT_FOUND);
            case 405 -> new ApiError(ApiErrorCodes.METHOD_NOT_ALLOWED, ClientMessages.METHOD_NOT_ALLOWED);
            case 406 -> new ApiError(ApiErrorCodes.NOT_ACCEPTABLE, ClientMessages.JSON_RESPONSE_REQUIRED);
            case 415 -> new ApiError(ApiErrorCodes.UNSUPPORTED_MEDIA_TYPE, ClientMessages.JSON_CONTENT_TYPE_REQUIRED);
            default -> new ApiError(ApiErrorCodes.REQUEST_FAILED, ClientMessages.REQUEST_FAILED);
        };
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setCacheControl(CacheControl.noStore());
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(error, responseHeaders, status);
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noStore())
                .body(new ApiError(code, message));
    }
}
