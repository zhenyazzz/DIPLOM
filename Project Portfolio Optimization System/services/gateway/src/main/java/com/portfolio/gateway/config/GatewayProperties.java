package com.portfolio.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Gateway-owned settings ({@code gateway.*}). Routes live under {@code spring.cloud.gateway.server.webmvc.routes}.
 */
@Validated
@ConfigurationProperties("gateway")
public record GatewayProperties(
        @Valid @NotNull @DefaultValue Cors cors,
        @Valid @NotNull @DefaultValue LoginRateLimit loginRateLimit) {

    /** Browser origins allowed to call {@code /api/**} with credentials (refresh cookie). */
    public record Cors(@NotNull @DefaultValue("http://localhost:5173") List<String> allowedOrigins) {}

    /**
     * Fixed-window limit for {@code POST /api/v1/auth/login}, keyed by client IP.
     *
     * @param capacity max attempts per window
     * @param window   window length
     */
    public record LoginRateLimit(
            @Positive @DefaultValue("5") int capacity,
            @NotNull @DefaultValue("1m") Duration window) {}
}
