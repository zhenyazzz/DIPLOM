package com.portfolio.identity.auth;

import com.portfolio.identity.shared.AuthProperties;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Active access sessions in Redis. A valid JWT is not enough – the jti must exist here with TTL.
 */
@Service
@RequiredArgsConstructor
public class AccessSessionService {

    private static final String SESSION_KEY_PREFIX = "identity:access-session:";
    private static final String USER_INDEX_PREFIX = "identity:access-user:";

    private final StringRedisTemplate redis;
    private final AuthProperties authProperties;

    public void create(String jti, UUID userId, String role) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("jti must not be blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        Duration ttl = authProperties.getJwt().getAccessTokenTtl();
        String sessionKey = sessionKey(jti);
        redis.opsForValue().set(sessionKey, userId + "|" + role, ttl);

        String userKey = userKey(userId);
        redis.opsForSet().add(userKey, jti);
        // Keep the index at least as long as a fresh access token; stale jtis are harmless on revoke.
        redis.expire(userKey, ttl);
    }

    public boolean isActive(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Boolean exists = redis.hasKey(sessionKey(jti));
        return Boolean.TRUE.equals(exists);
    }

    public SessionPayload requireActive(String jti) {
        if (jti == null || jti.isBlank()) {
            return null;
        }
        String payload = redis.opsForValue().get(sessionKey(jti));
        if (payload == null || payload.isBlank()) {
            return null;
        }
        int sep = payload.indexOf('|');
        if (sep <= 0 || sep >= payload.length() - 1) {
            return null;
        }
        try {
            return new SessionPayload(UUID.fromString(payload.substring(0, sep)), payload.substring(sep + 1));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void revoke(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        String payload = redis.opsForValue().getAndDelete(sessionKey(jti));
        if (payload == null) {
            return;
        }
        int sep = payload.indexOf('|');
        if (sep > 0) {
            try {
                UUID userId = UUID.fromString(payload.substring(0, sep));
                redis.opsForSet().remove(userKey(userId), jti);
            } catch (IllegalArgumentException ignored) {
                // corrupt payload – session key already removed
            }
        }
    }

    public void revokeAllForUser(UUID userId) {
        if (userId == null) {
            return;
        }
        String userKey = userKey(userId);
        Set<String> jtis = redis.opsForSet().members(userKey);
        if (jtis != null) {
            for (String jti : jtis) {
                redis.delete(sessionKey(jti));
            }
        }
        redis.delete(userKey);
    }

    private static String sessionKey(String jti) {
        return SESSION_KEY_PREFIX + jti;
    }

    private static String userKey(UUID userId) {
        return USER_INDEX_PREFIX + userId;
    }

    public record SessionPayload(UUID userId, String role) {
    }
}
