package com.longdnh.donot_get_left_behind.common.Exception;

import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Servlet-level boundary before Spring Security; registered once by Spring Boot. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public final class ExceptionHandlingFilter extends OncePerRequestFilter {

    private final ObjectProvider<HandlerExceptionResolver> exceptionResolver;
    private final ApiErrorWriter errorWriter;

    public ExceptionHandlingFilter(
            @Qualifier("handlerExceptionResolver") ObjectProvider<HandlerExceptionResolver> exceptionResolver,
            ApiErrorWriter errorWriter) {
        this.exceptionResolver = exceptionResolver;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (IOException exception) {
            // An I/O failure can mean the client disconnected; do not attempt another response.
            throw exception;
        } catch (Exception exception) {
            if (response.isCommitted()) {
                throw exception;
            }
            clearBody(response);
            Exception failure = unwrapServletException(exception);
            try {
                if (exceptionResolver.getObject().resolveException(request, response, null, failure) != null) {
                    return;
                }
            } catch (RuntimeException resolverFailure) {
                logger.error("Exception resolver failure type: " + resolverFailure.getClass().getName());
            }
            if (response.isCommitted()) {
                throw exception;
            }
            logger.error("Unresolved request exception type: " + failure.getClass().getName());
            clearBody(response);
            errorWriter.write(response, 500, ApiErrorCodes.INTERNAL_ERROR, ClientMessages.REQUEST_FAILED);
        }
    }

    private static void clearBody(HttpServletResponse response) {
        response.resetBuffer();
        response.setHeader(HttpHeaders.CONTENT_LENGTH, null);
    }

    private static Exception unwrapServletException(Exception exception) {
        Exception current = exception;
        while (current instanceof ServletException && current.getCause() instanceof Exception cause
                && cause != current) {
            current = cause;
        }
        return current;
    }
}
