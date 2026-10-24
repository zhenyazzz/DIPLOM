package com.portfolio.common.web.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        ProjectStatus status,
        BigDecimal budget,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
