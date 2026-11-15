package com.portfolio.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.common.security.jwt.AccessTokenClaims;
import com.portfolio.common.security.jwt.AccessTokenVerifier;
import com.portfolio.common.security.support.TestJwtFactory;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class AccessTokenVerifierTest {

    private AccessTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        byte[] secret = TestJwtFactory.DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8);
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        verifier = new AccessTokenVerifier(decoder, "role");
    }

    @Test
    void verifyValidTokenReturnsClaims() {
        UUID subject = UUID.randomUUID();
        String token = TestJwtFactory.validUserToken(subject);

        AccessTokenClaims claims = verifier.verify(token);

        assertThat(claims.subject()).isEqualTo(subject);
        assertThat(claims.role()).isEqualTo("USER");
        assertThat(claims.roles()).containsExactly("USER");
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.expiresAt()).isNotNull();
    }

    @Test
    void verifyExpiredTokenFails() {
        String token = TestJwtFactory.expiredToken(UUID.randomUUID());
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void verifyBadSignatureFails() {
        String token = TestJwtFactory.wrongSecretToken(UUID.randomUUID());
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyMissingSubjectFails() {
        String token = TestJwtFactory.tokenWithoutSubject();
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }
}
