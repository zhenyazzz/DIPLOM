package com.portfolio.domain.api.project;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProjectCardResponse(
        UUID id,
        String code,
        String title,
        String description,
        String status,
        BigDecimal requestedBudget,
        DepartmentSummaryResponse department,
        UUID ownerId,
        List<GoalContributionResponse> goals) {}
