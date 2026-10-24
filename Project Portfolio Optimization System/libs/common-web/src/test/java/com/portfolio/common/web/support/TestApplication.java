package com.portfolio.common.web.support;

import com.portfolio.common.web.config.EnableCommonWeb;
import java.util.Optional;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableCommonWeb
@EnableJpaAuditing
public class TestApplication {

    @Bean
    AuditorAware<String> auditorAware() {
        return () -> Optional.of("test-user");
    }
}
