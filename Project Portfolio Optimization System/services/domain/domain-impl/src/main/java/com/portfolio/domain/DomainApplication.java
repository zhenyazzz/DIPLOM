package com.portfolio.domain;

import com.portfolio.common.web.config.EnableCommonWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCommonWeb(problemDetails = false)
@EnableJpaAuditing
@EnableAsync
public class DomainApplication {

    public static void main(String[] args) {
        SpringApplication.run(DomainApplication.class, args);
    }
}
