package com.portfolio.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.common.security.context.SecuritySupport;
import com.portfolio.common.security.context.UnauthenticatedException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecuritySupportTest {

    private final SecuritySupport securitySupport = new SecuritySupport("role");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireUserIdAndRoleWhenAuthenticated() {
        UUID userId = UUID.randomUUID();
        setJwt(userId, "ADMIN");

        assertThat(securitySupport.requireUserId()).isEqualTo(userId);
        assertThat(securitySupport.requireRole()).isEqualTo("ADMIN");
    }

    @Test
    void requireUserIdFailsWithoutAuthentication() {
        assertThatThrownBy(securitySupport::requireUserId)
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessageContaining("No authenticated principal");
    }

    @Test
    void requireRoleFailsWithoutAuthentication() {
        assertThatThrownBy(securitySupport::requireRole)
                .isInstanceOf(UnauthenticatedException.class);
    }

    private static void setJwt(UUID subject, String role) {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", subject.toString(), "role", role));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
