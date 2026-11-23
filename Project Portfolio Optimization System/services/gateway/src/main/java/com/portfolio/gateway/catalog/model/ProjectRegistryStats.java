package com.portfolio.gateway.catalog.model;

import java.util.List;

public record ProjectRegistryStats(
        int totalCount,
        List<StatusCount> byStatus,
        String totalRequestedBudget,
        List<DepartmentBudgetStat> budgetByDepartment) {}
