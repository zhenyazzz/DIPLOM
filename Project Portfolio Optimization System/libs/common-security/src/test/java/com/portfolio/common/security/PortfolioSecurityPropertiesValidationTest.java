package com.portfolio.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.common.security.support.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

class PortfolioSecurityPropertiesValidationTest {

    @Test
    void blankSecretFailsStartup() {
        assertThatThrownBy(() -> runWith(
                        "--server.port=0",
                        "--portfolio.security.jwt.secret="))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .rootCause()
                .hasMessageContaining("must not be blank");
    }

    @Test
    void shortSecretFailsStartup() {
        assertThatThrownBy(() -> runWith(
                        "--server.port=0",
                        "--portfolio.security.jwt.secret=too-short"))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .rootCause()
                .hasMessageContaining("at least 32 UTF-8 bytes");
    }

    private static void runWith(String... args) {
        SpringApplication app = new SpringApplication(TestApplication.class);
        app.setWebApplicationType(WebApplicationType.SERVLET);
        try (ConfigurableApplicationContext ignored = app.run(args)) {
        }
    }
}
