package com.portfolio.common.security.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class TestJwtFactory {

    public static final String DEFAULT_SECRET = "test-secret-that-is-long-enough-32b!";

    private TestJwtFactory() {}

    public static String mint(UUID subject, String role, String email, Instant expiresAt) {
        return mint(DEFAULT_SECRET, subject, role, email, Instant.now(), expiresAt);
    }

    public static String mint(
            String secret,
            UUID subject,
            String role,
            String email,
            Instant issuedAt,
            Instant expiresAt) {
        try {
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(subject.toString())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt));
            if (role != null) {
                builder.claim("role", role);
            }
            if (email != null) {
                builder.claim("email", email);
            }
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to mint test JWT", e);
        }
    }

    public static String validUserToken(UUID subject) {
        return mint(subject, "USER", "user@example.com", Instant.now().plusSeconds(3600));
    }

    public static String validAdminToken(UUID subject) {
        return mint(subject, "ADMIN", "admin@example.com", Instant.now().plusSeconds(3600));
    }

    public static String expiredToken(UUID subject) {
        Instant past = Instant.now().minusSeconds(3600);
        return mint(DEFAULT_SECRET, subject, "USER", "user@example.com", past.minusSeconds(60), past);
    }

    public static String wrongSecretToken(UUID subject) {
        return mint(
                "other-secret-that-is-long-enough-32b!",
                subject,
                "USER",
                "user@example.com",
                Instant.now(),
                Instant.now().plusSeconds(3600));
    }

    public static String tokenWithoutSubject() {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .claim("role", "USER")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to mint test JWT", e);
        }
    }
}
