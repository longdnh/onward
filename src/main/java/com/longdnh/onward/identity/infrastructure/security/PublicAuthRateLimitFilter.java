package com.longdnh.onward.identity.infrastructure.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import com.longdnh.onward.common.Exception.ApiErrorWriter;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Per-IP fixed window for the public POST endpoints that create accounts or send email
 * (register, resend verification, forgot password). Each path has its own counter per client, so
 * exhausting one does not block the others. Single-instance, in-memory; constructed only in the
 * security chain, not as a servlet filter bean.
 */
public final class PublicAuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, Attempts> clients = new HashMap<>();
    private final Clock clock;
    private final ApiErrorWriter errorWriter;
    private final Set<String> limitedPaths;
    private final int maxAttempts;
    private final Duration window;

    public PublicAuthRateLimitFilter(Clock clock, ApiErrorWriter errorWriter, Set<String> limitedPaths,
                                     int maxAttempts, Duration window) {
        if (maxAttempts < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limits must be positive");
        }
        if (limitedPaths.isEmpty()) {
            throw new IllegalArgumentException("At least one path must be rate limited");
        }
        this.clock = clock;
        this.errorWriter = errorWriter;
        this.limitedPaths = Set.copyOf(limitedPaths);
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !limitedPaths.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Use the connection address; never trust a caller-supplied X-Forwarded-For header here.
        String key = request.getServletPath() + "|" + request.getRemoteAddr();
        long retryAfter = acquire(key, clock.instant());
        if (retryAfter > 0) {
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
            errorWriter.write(response, 429, ApiErrorCodes.RATE_LIMITED, ClientMessages.RATE_LIMITED);
            return;
        }
        chain.doFilter(request, response);
    }

    private synchronized long acquire(String key, Instant now) {
        clients.values().removeIf(attempts -> !now.isBefore(attempts.expiresAt()));
        Attempts attempts = clients.get(key);
        if (attempts == null) {
            if (clients.size() >= MAX_TRACKED_KEYS) {
                Instant nextExpiry = clients.values().stream().map(Attempts::expiresAt)
                        .min(Instant::compareTo).orElseThrow();
                return retrySeconds(now, nextExpiry);
            }
            clients.put(key, new Attempts(1, now.plus(window)));
            return 0;
        }
        if (attempts.count() >= maxAttempts) {
            return retrySeconds(now, attempts.expiresAt());
        }
        clients.put(key, new Attempts(attempts.count() + 1, attempts.expiresAt()));
        return 0;
    }

    private static long retrySeconds(Instant now, Instant expiresAt) {
        return Math.max(1, Duration.between(now, expiresAt).getSeconds());
    }

    private record Attempts(int count, Instant expiresAt) {
    }
}
