package com.portfolio.gateway.security;

import com.portfolio.gateway.config.GatewayProperties;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class RestIdentitySessionClient implements IdentitySessionClient {

    private final RestClient restClient;

    public RestIdentitySessionClient(GatewayProperties properties, RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(properties.identityUrl())
                .build();
    }

    @Override
    public boolean isSessionActive(String rawAccessToken) {
        if (rawAccessToken == null || rawAccessToken.isBlank()) {
            return false;
        }
        try {
            restClient
                    .get()
                    .uri("/api/v1/auth/sessions/active")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + rawAccessToken)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 401 || status.value() == 403) {
                return false;
            }
            log.warn("Identity session check failed with HTTP {}: {}", status.value(), ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            log.warn("Identity session check failed: {}", ex.getMessage());
            return false;
        }
    }
}
