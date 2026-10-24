package com.portfolio.common.web.support;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record UpdateProjectRequest(
        String name,
        ProjectStatus status,
        @DecimalMin("0.01") BigDecimal budget
) {
}
