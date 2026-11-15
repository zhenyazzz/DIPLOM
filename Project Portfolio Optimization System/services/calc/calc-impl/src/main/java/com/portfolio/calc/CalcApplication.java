package com.portfolio.calc;

import com.portfolio.common.web.config.EnableCommonWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCommonWeb(problemDetails = false)
public class CalcApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalcApplication.class, args);
    }
}
