package com.portfolio.gateway.catalog;

import java.util.List;
import java.util.UUID;

public record ProjectCard(
        UUID id,
        String code,
        String title,
        String description,
        ProjectStatus status,
        String requestedBudget,
        DepartmentCard department,
        List<GoalContributionCard> goals,
        ParticipantCard initiator) {}
