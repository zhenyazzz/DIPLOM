package com.portfolio.gateway.catalog;

import java.util.List;

public record ProjectFilter(
        ProjectStatus status,
        List<ProjectStatus> statuses,
        String departmentCode,
        String minBudget,
        String maxBudget,
        String goalCode,
        Boolean excludeArchived) {}
