package com.portfolio.common.security.jwt;

import java.time.Instant;
import java.util.UUID;

public record AccessTokenClaims(
        UUID subject,
        String role,
        String email,
        Instant issuedAt,
        Instant expiresAt) {}
