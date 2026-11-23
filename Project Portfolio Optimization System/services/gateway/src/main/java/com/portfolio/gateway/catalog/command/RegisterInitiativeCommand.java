package com.portfolio.gateway.catalog.command;

import java.util.List;

public record RegisterInitiativeCommand(
        String code,
        String title,
        String description,
        String departmentCode,
        String ownerId,
        String requestedBudget,
        List<GoalContributionInput> goals) {}
