package com.portfolio.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.identity.shared.AuthProperties;
import com.portfolio.identity.support.PostgresIntegrationTest;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis session lifecycle against the shared Testcontainers Redis from {@link PostgresIntegrationTest}.
 */
class AccessSessionServiceTest extends PostgresIntegrationTest {

    @Autowired
    StringRedisTemplate redis;

    private AccessSessionService sessions;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.getJwt().setAccessTokenTtl(Duration.ofSeconds(30));
        sessions = new AccessSessionService(redis, props);
    }

    @Test
    void createAndRequireActive() {
        String jti = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        assertThat(sessions.isActive(jti)).isFalse();

        sessions.create(jti, userId, "INITIATOR");
        assertThat(sessions.isActive(jti)).isTrue();
        assertThat(sessions.requireActive(jti).userId()).isEqualTo(userId);
        assertThat(sessions.requireActive(jti).role()).isEqualTo("INITIATOR");
    }

    @Test
    void revokeAllForUserRemovesEverySession() {
        UUID userId = UUID.randomUUID();
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();

        sessions.create(first, userId, "ADMIN");
        sessions.create(second, userId, "ADMIN");
        assertThat(sessions.isActive(first)).isTrue();
        assertThat(sessions.isActive(second)).isTrue();

        sessions.revokeAllForUser(userId);
        assertThat(sessions.isActive(first)).isFalse();
        assertThat(sessions.isActive(second)).isFalse();
    }
}
