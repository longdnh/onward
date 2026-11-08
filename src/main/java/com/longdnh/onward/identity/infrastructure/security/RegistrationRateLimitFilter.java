package com.longdnh.donot_get_left_behind.identity.infrastructure.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import com.longdnh.donot_get_left_behind.common.Exception.ApiErrorWriter;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Single-instance fixed windows. Constructed only in the security chain, not as a servlet filter bean. */
public final class RegistrationRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final Map<String, Attempts> clients = new HashMap<>();
    private final Clock clock;
    private final ApiErrorWriter errorWriter;
    private final int maxAttempts;
    private final Duration window;

    public RegistrationRateLimitFilter(Clock clock, ApiErrorWriter errorWriter, int maxAttempts, Duration window) {
        if (maxAttempts < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Registration rate limits must be positive");
        }
        this.clock = clock;
        this.errorWriter = errorWriter;
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !"/api/auth/register".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Use the connection address; never trust a caller-supplied X-Forwarded-For header here.
        long retryAfter = acquire(request.getRemoteAddr(), clock.instant());
        if (retryAfter > 0) {
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
            errorWriter.write(response, 429, ApiErrorCodes.RATE_LIMITED, ClientMessages.REGISTRATION_RATE_LIMITED);
            return;
        }
        chain.doFilter(request, response);
    }

    private synchronized long acquire(String address, Instant now) {
        clients.values().removeIf(attempts -> !now.isBefore(attempts.expiresAt()));
        Attempts attempts = clients.get(address);
        if (attempts == null) {
            if (clients.size() >= MAX_TRACKED_CLIENTS) {
                Instant nextExpiry = clients.values().stream().map(Attempts::expiresAt)
                        .min(Instant::compareTo).orElseThrow();
                return retrySeconds(now, nextExpiry);
            }
            clients.put(address, new Attempts(1, now.plus(window)));
            return 0;
        }
        if (attempts.count() >= maxAttempts) {
            return retrySeconds(now, attempts.expiresAt());
        }
        clients.put(address, new Attempts(attempts.count() + 1, attempts.expiresAt()));
        return 0;
    }

    private static long retrySeconds(Instant now, Instant expiresAt) {
        Duration remaining = Duration.between(now, expiresAt);
        return Math.max(1, remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0));
    }

    private record Attempts(int count, Instant expiresAt) {
    }
}
