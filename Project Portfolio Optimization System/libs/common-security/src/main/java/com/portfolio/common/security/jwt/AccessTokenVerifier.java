package com.portfolio.common.security.jwt;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@RequiredArgsConstructor
public class AccessTokenVerifier {

    private final JwtDecoder jwtDecoder;
    private final String roleClaim;

    public AccessTokenClaims verify(String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Token must not be blank");
        }
        try {
            Jwt jwt = jwtDecoder.decode(rawJwt);
            String subject = jwt.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new IllegalArgumentException("JWT subject (sub) is missing");
            }
            UUID userId;
            try {
                userId = UUID.fromString(subject);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("JWT subject (sub) is not a valid UUID: " + subject, ex);
            }
            return new AccessTokenClaims(
                    userId,
                    jwt.getClaimAsString(roleClaim),
                    jwt.getClaimAsString("email"),
                    jwt.getIssuedAt(),
                    jwt.getExpiresAt());
        } catch (JwtException ex) {
            throw new IllegalArgumentException("Invalid token: " + ex.getMessage(), ex);
        }
    }
}
