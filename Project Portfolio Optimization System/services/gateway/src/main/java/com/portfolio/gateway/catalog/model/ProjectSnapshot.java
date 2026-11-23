package com.portfolio.gateway.catalog.model;

import java.util.List;
import java.util.UUID;

public record ProjectSnapshot(
        UUID id,
        String code,
        String title,
        String description,
        ProjectStatus status,
        String requestedBudget,
        DepartmentCard department,
        UUID ownerId,
        List<GoalContributionCard> goals) {}
