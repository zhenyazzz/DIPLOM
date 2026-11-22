package com.portfolio.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jwt.JWTClaimsSet;
import com.portfolio.identity.api.identity.UserRole;
import com.portfolio.identity.identity.RoleEntity;
import com.portfolio.identity.identity.UserEntity;
import com.portfolio.identity.shared.AuthProperties;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.getJwt().setSecret("test-jwt-secret-at-least-32-bytes-long!!");
        props.getJwt().setAccessTokenTtl(Duration.ofMinutes(15));
        jwtTokenService = new JwtTokenService(props);
    }

    @Test
    void issueAndParse_roundTripClaims() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setRole(role(UserRole.INITIATOR));

        JwtTokenService.IssuedAccessToken issued = jwtTokenService.issueAccessToken(user);
        JWTClaimsSet claims = jwtTokenService.parse(issued.token());

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.getJWTID()).isEqualTo(issued.jti());
        assertThat(claims.getClaim("role")).isEqualTo("INITIATOR");
        assertThat(claims.getClaim("email")).isEqualTo("alice@example.com");
        assertThat(claims.getIssueTime()).isNotNull();
        assertThat(claims.getExpirationTime()).isNotNull();
        assertThat(jwtTokenService.accessExpiresInSeconds()).isEqualTo(900);
    }

    @Test
    void parse_rejectsTamperedToken() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("bob@example.com");
        user.setRole(role(UserRole.ADMIN));

        String token = jwtTokenService.issueAccessToken(user).token();
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThatThrownBy(() -> jwtTokenService.parse(tampered))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsShortSecret() {
        AuthProperties props = new AuthProperties();
        props.getJwt().setSecret("too-short");
        assertThatThrownBy(() -> new JwtTokenService(props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    private static RoleEntity role(UserRole code) {
        RoleEntity entity = new RoleEntity();
        entity.setId(UUID.randomUUID());
        entity.setCode(code.name());
        entity.setName(code.name());
        return entity;
    }
}
