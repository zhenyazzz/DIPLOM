package com.portfolio.identity.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.portfolio.identity.identity.UserEntity;
import com.portfolio.identity.shared.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final AuthProperties authProperties;
    private final byte[] secretBytes;

    public JwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.secretBytes = authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
    }

    public String issueAccessToken(UserEntity user) {
        Instant now = Instant.now();
        Instant exp = now.plus(authProperties.getJwt().getAccessTokenTtl());
        if (user.getRole() == null || user.getRole().getCode() == null) {
            throw new IllegalStateException("User has no role assigned: " + user.getId());
        }
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .claim("role", user.getRole().getCode())
                    .claim("email", user.getEmail())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secretBytes));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
    }

    public long accessExpiresInSeconds() {
        return authProperties.getJwt().getAccessTokenTtl().toSeconds();
    }

    public SecretKey secretKey() {
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    /**
     * Parse and verify an access JWT (unit / diagnostic use). Resource server uses JwtDecoder.
     */
    public JWTClaimsSet parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secretBytes))) {
                throw new IllegalArgumentException("Invalid token signature");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Token expired");
            }
            Objects.requireNonNull(claims.getSubject(), "sub");
            return claims;
        } catch (ParseException | JOSEException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    public UUID subjectAsUuid(JWTClaimsSet claims) {
        return UUID.fromString(claims.getSubject());
    }
}
