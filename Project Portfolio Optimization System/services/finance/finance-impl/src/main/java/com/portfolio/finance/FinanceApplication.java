package com.portfolio.finance;

import com.portfolio.common.web.config.EnableCommonWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableCommonWeb(problemDetails = false)
@EnableJpaAuditing
public class FinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
