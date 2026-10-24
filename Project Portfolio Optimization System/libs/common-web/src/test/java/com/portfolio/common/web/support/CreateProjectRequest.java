package com.portfolio.common.web.support;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateProjectRequest(
        @NotBlank String name,
        @NotNull ProjectStatus status,
        @NotNull @DecimalMin("0.01") BigDecimal budget
) {
}
