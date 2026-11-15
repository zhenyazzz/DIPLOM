package com.portfolio.common.security.autoconfigure;

import com.portfolio.common.security.audit.JwtAuditorAware;
import com.portfolio.common.security.autoconfigure.PortfolioSecurityProperties.PermitAllEntry;
import com.portfolio.common.security.context.SecuritySupport;
import com.portfolio.common.security.jwt.AccessTokenVerifier;
import com.portfolio.common.security.jwt.JwtRoleClaims;
import com.portfolio.common.security.web.ProblemDetailsAccessDeniedHandler;
import com.portfolio.common.security.web.ProblemDetailsAuthEntryPoint;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(before = OAuth2ResourceServerWebSecurityAutoConfiguration.class)
@EnableConfigurationProperties(PortfolioSecurityProperties.class)
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PortfolioSecurityAutoConfiguration {

    public PortfolioSecurityAutoConfiguration(PortfolioSecurityProperties properties) {
        properties.validate();
    }

    @Bean
    @ConditionalOnMissingBean
    JwtDecoder jwtDecoder(PortfolioSecurityProperties properties) {
        byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter jwtAuthenticationConverter(PortfolioSecurityProperties properties) {
        String roleClaim = properties.getJwt().getRoleClaim();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = JwtRoleClaims.extract(jwt, roleClaim);
            if (roles.isEmpty()) {
                return List.of();
            }
            return roles.stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    AuthenticationEntryPoint problemDetailsAuthEntryPoint(JsonMapper jsonMapper) {
        return new ProblemDetailsAuthEntryPoint(jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    AccessDeniedHandler problemDetailsAccessDeniedHandler(JsonMapper jsonMapper) {
        return new ProblemDetailsAccessDeniedHandler(jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "portfolio.security.filter-chain",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    SecurityFilterChain portfolioSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler,
            PortfolioSecurityProperties properties) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    for (String raw : properties.getPermitAll()) {
                        PermitAllEntry entry = PortfolioSecurityProperties.parsePermitAllEntry(raw);
                        auth.requestMatchers(HttpMethod.valueOf(entry.method()), entry.pathPattern())
                                .permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    SecuritySupport securitySupport(PortfolioSecurityProperties properties) {
        return new SecuritySupport(properties.getJwt().getRoleClaim());
    }

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    AuditorAware<String> jwtAuditorAware() {
        return new JwtAuditorAware();
    }

    @Bean
    @ConditionalOnMissingBean
    AccessTokenVerifier accessTokenVerifier(
            JwtDecoder jwtDecoder, PortfolioSecurityProperties properties) {
        return new AccessTokenVerifier(jwtDecoder, properties.getJwt().getRoleClaim());
    }
}
