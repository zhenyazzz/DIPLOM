package com.portfolio.notify;

import com.portfolio.common.web.config.EnableCommonWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableCommonWeb(problemDetails = false)
@EnableJpaAuditing
public class NotifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyApplication.class, args);
    }
}
