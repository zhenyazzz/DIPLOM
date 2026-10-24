package com.portfolio.common.security.autoconfigure;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Getter
@ConfigurationProperties(prefix = "portfolio.security")
public class PortfolioSecurityProperties {

    private static final Set<String> HTTP_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE");

    @NestedConfigurationProperty
    private final Jwt jwt = new Jwt();

    private List<String> permitAll = new ArrayList<>();

    @NestedConfigurationProperty
    private final FilterChain filterChain = new FilterChain();

    public void setPermitAll(List<String> permitAll) {
        this.permitAll = permitAll != null ? permitAll : new ArrayList<>();
    }

    public void validate() {
        String secret = jwt.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("portfolio.security.jwt.secret must not be blank");
        }
        int secretBytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (secretBytes < 32) {
            throw new IllegalStateException(
                    "portfolio.security.jwt.secret must be at least 32 UTF-8 bytes (was " + secretBytes + ")");
        }
        String roleClaim = jwt.getRoleClaim();
        if (roleClaim == null || roleClaim.isBlank()) {
            throw new IllegalStateException("portfolio.security.jwt.role-claim must not be blank");
        }
        for (String entry : permitAll) {
            parsePermitAllEntry(entry);
        }
    }

    public static PermitAllEntry parsePermitAllEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            throw new IllegalStateException("portfolio.security.permit-all entry must not be blank");
        }
        int colon = entry.indexOf(':');
        if (colon <= 0 || colon == entry.length() - 1) {
            throw new IllegalStateException(
                    "portfolio.security.permit-all entry must be METHOD:pathPattern, got: " + entry);
        }
        String method = entry.substring(0, colon).trim().toUpperCase(Locale.ROOT);
        String pattern = entry.substring(colon + 1).trim();
        if (!HTTP_METHODS.contains(method)) {
            throw new IllegalStateException(
                    "portfolio.security.permit-all unknown HTTP method '" + method + "' in: " + entry);
        }
        if (pattern.isEmpty()) {
            throw new IllegalStateException("portfolio.security.permit-all path pattern missing in: " + entry);
        }
        return new PermitAllEntry(method, pattern);
    }

    public record PermitAllEntry(String method, String pathPattern) {}

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private String roleClaim = "role";
    }

    @Getter
    @Setter
    public static class FilterChain {
        private boolean enabled = true;
    }
}
