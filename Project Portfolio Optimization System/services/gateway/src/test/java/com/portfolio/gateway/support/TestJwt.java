package com.portfolio.gateway.support;

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

public final class TestJwt {

    /** Must match {@code portfolio.security.jwt.secret} in {@code application-test.yml}. */
    public static final String SECRET = "gateway-integration-test-jwt-secret-at-least-40-bytes!!";

    private TestJwt() {}

    public static String userToken(UUID subject) {
        return mint(subject, "INITIATOR", "user@example.com", 3600);
    }

    public static String expiredToken(UUID subject) {
        return mint(subject, "INITIATOR", "user@example.com", -60);
    }

    private static String mint(UUID subject, String role, String email, long ttlSeconds) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject.toString())
                    .claim("role", role)
                    .claim("email", email)
                    .issueTime(Date.from(now.minusSeconds(120)))
                    .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed to mint test JWT", ex);
        }
    }
}
