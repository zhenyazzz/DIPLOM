package com.portfolio.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.common.security.audit.JwtAuditorAware;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtAuditorAwareTest {

    private final JwtAuditorAware auditorAware = new JwtAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsSubjectWhenAuthenticated() {
        UUID subject = UUID.randomUUID();
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", subject.toString(), "role", "USER"));
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(
                        jwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(auditorAware.getCurrentAuditor()).contains(subject.toString());
    }

    @Test
    void returnsEmptyWhenAnonymous() {
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoAuthentication() {
        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }
}
