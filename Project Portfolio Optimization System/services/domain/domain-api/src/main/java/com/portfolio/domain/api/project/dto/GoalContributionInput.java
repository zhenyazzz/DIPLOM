package com.portfolio.domain.api.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GoalContributionInput(
        @NotNull UUID goalId,
        @Min(1) @Max(5) int level) {}
