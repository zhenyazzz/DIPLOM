package com.portfolio.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.common.security.context.SecuritySupport;
import com.portfolio.common.security.jwt.AccessTokenVerifier;
import com.portfolio.common.security.support.TestApplication;
import com.portfolio.common.security.support.TestJwtFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "portfolio.security.filter-chain.enabled=false")
@Import(FilterChainOptOutTest.ConsumerSecurityConfig.class)
class FilterChainOptOutTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private SecuritySupport securitySupport;

    @Autowired
    private AccessTokenVerifier accessTokenVerifier;

    @Autowired
    private AuditorAware<String> auditorAware;

    @Test
    void libraryHelpersRemainAvailableWhenFilterChainDisabled() {
        assertThat(jwtDecoder).isNotNull();
        assertThat(securitySupport).isNotNull();
        assertThat(accessTokenVerifier).isNotNull();
        assertThat(auditorAware).isNotNull();
    }

    @Test
    void consumerOwnedChainCanProtectEndpoints() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtFactory.validUserToken(userId);

        mockMvc.perform(get("/api/protected/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/protected/me")).andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class ConsumerSecurityConfig {

        @Bean
        SecurityFilterChain consumerSecurityFilterChain(
                HttpSecurity http,
                JwtDecoder jwtDecoder,
                JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/public/**")
                            .permitAll()
                            .anyRequest()
                            .authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                            .decoder(jwtDecoder)
                            .jwtAuthenticationConverter(jwtAuthenticationConverter)));
            return http.build();
        }
    }
}
