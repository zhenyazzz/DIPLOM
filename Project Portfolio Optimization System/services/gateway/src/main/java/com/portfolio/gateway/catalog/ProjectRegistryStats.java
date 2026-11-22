package com.portfolio.gateway.catalog;

import java.util.List;

public record ProjectRegistryStats(
        int totalCount,
        List<StatusCount> byStatus,
        String totalRequestedBudget,
        List<DepartmentBudgetStat> budgetByDepartment) {}
