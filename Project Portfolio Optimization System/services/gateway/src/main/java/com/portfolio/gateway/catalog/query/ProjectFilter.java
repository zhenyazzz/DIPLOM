package com.portfolio.gateway.catalog.query;

import com.portfolio.gateway.catalog.model.ProjectStatus;
import java.util.List;

public record ProjectFilter(
        ProjectStatus status,
        List<ProjectStatus> statuses,
        String departmentCode,
        String minBudget,
        String maxBudget,
        String goalCode,
        Boolean excludeArchived) {}
