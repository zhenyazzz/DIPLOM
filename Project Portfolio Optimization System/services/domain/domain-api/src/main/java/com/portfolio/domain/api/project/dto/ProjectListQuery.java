package com.portfolio.domain.api.project.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProjectListQuery(
        String status,
        List<String> statuses,
        String departmentCode,
        BigDecimal minBudget,
        BigDecimal maxBudget,
        String goalCode,
        Boolean excludeArchived,
        String sort,
        String direction) {

    public static ProjectListQuery ofStatus(String status) {
        return new ProjectListQuery(status, null, null, null, null, null, false, null, null);
    }
}
