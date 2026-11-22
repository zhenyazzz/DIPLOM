package com.portfolio.domain.api.project.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProjectRegistryStatsResponse(
        long totalCount,
        List<StatusCountResponse> byStatus,
        BigDecimal totalRequestedBudget,
        List<DepartmentBudgetStatResponse> budgetByDepartment) {}
