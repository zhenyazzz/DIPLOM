package com.portfolio.common.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableSpringDataWebSupport
public class CommonWebConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommonWebProperties commonWebProperties() {
        return new CommonWebProperties();
    }
}
