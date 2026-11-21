package com.longdnh.onward.identity.infrastructure.denylist;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.longdnh.onward.identity.application.port.SessionDenylist;
import com.longdnh.onward.identity.infrastructure.configuration.AuthProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Key contract shared with the gateway (docs/microservice-decomposition.md, 7.2):
 * <ul>
 * <li>{@code denied:session:<sessionId>} = "1": reject any token whose {@code sid} matches.</li>
 * <li>{@code denied:user:<userId>} = epoch seconds: reject any token whose {@code sub} matches and whose
 * {@code iat} is not after that instant.</li>
 * </ul>
 * Both expire after the access-token lifetime: by then every token issued before the revocation has
 * expired on its own, so the entry has nothing left to block.
 */
@Slf4j
@Component
public class RedisSessionDenylist implements SessionDenylist {

    static final String SESSION_KEY_PREFIX = "denied:session:";
    static final String USER_KEY_PREFIX = "denied:user:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public RedisSessionDenylist(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.ttl = properties.accessTokenLifetime();
    }

    @Override
    public void denySession(UUID sessionId) {
        write(SESSION_KEY_PREFIX + sessionId, "1");
    }

    @Override
    public void denyUserTokensIssuedBefore(UUID userId, Instant revokedAt) {
        write(USER_KEY_PREFIX + userId, Long.toString(revokedAt.getEpochSecond()));
    }

    private void write(String key, String value) {
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (DataAccessException exception) {
            // Decided fail-open (section 7): the revocation is committed in PostgreSQL; only the early
            // rejection at the gateway is lost, for at most one access-token lifetime.
            log.error("Denylist write failed for {}: {}", key, exception.getClass().getName());
        }
    }
}
