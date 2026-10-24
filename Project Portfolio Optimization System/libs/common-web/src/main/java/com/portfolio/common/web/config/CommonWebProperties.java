package com.portfolio.common.web.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommonWebProperties {

    private int defaultPageSize = 20;
    private int maxPageSize = 100;
}
