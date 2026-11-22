package com.portfolio.domain.api.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegisterProjectRequest(
        @NotBlank String code,
        @NotBlank String title,
        String description,
        @NotNull UUID departmentId,
        @NotNull UUID ownerId,
        @NotNull @DecimalMin("0") BigDecimal requestedBudget,
        @NotEmpty List<@Valid GoalContributionInput> goals) {}
