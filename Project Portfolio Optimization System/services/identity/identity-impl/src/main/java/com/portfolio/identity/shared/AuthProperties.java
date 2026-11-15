package com.portfolio.identity.shared;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "app")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Cookie cookie = new Cookie();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "";
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private Duration refreshTokenTtl = Duration.ofDays(14);
    }

    @Getter
    @Setter
    public static class Cookie {
        private boolean secure;
    }
}
