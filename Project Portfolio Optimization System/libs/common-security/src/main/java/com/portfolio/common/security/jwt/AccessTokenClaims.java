package com.portfolio.common.security.jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccessTokenClaims(
        UUID subject,
        String role,
        List<String> roles,
        String email,
        Instant issuedAt,
        Instant expiresAt) {

    public AccessTokenClaims {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
