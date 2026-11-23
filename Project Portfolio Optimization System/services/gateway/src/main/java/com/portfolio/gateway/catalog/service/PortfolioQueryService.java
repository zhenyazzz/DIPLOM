package com.portfolio.gateway.catalog.service;

import com.portfolio.gateway.catalog.exception.InvalidProjectIdException;
import com.portfolio.gateway.catalog.exception.ProjectNotFoundException;
import com.portfolio.gateway.catalog.model.GoalContributionCard;
import com.portfolio.gateway.catalog.model.ParticipantCard;
import com.portfolio.gateway.catalog.model.ProjectCard;
import com.portfolio.gateway.catalog.model.ProjectRegistryStats;
import com.portfolio.gateway.catalog.model.ProjectSnapshot;
import com.portfolio.gateway.catalog.port.DomainProjectCatalog;
import com.portfolio.gateway.catalog.port.ParticipantDirectory;
import com.portfolio.gateway.catalog.query.ProjectFilter;
import com.portfolio.gateway.catalog.query.ProjectSort;
import com.portfolio.gateway.catalog.query.SortDirection;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioQueryService {

    private final DomainProjectCatalog projects;
    private final ParticipantDirectory participants;

    public ProjectCard project(String rawId) {
        UUID id = parseId(rawId);
        ProjectSnapshot snapshot = projects
                .findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return toCard(snapshot);
    }

    public List<ProjectCard> projects(ProjectFilter filter, ProjectSort sort) {
        return projects.findAll(filter).stream()
                .map(this::toCard)
                .sorted(comparator(sort))
                .toList();
    }

    public ProjectRegistryStats projectRegistryStats(ProjectFilter filter) {
        return projects.stats(filter);
    }

    private ProjectCard toCard(ProjectSnapshot snapshot) {
        ParticipantCard initiator = participants.findById(snapshot.ownerId()).orElse(null);
        List<GoalContributionCard> goals = snapshot.goals().stream()
                .sorted(Comparator.comparing(item -> item.goal().code()))
                .toList();
        return new ProjectCard(
                snapshot.id(),
                snapshot.code(),
                snapshot.title(),
                snapshot.description(),
                snapshot.status(),
                snapshot.requestedBudget(),
                snapshot.department(),
                goals,
                initiator);
    }

    private static Comparator<ProjectCard> comparator(ProjectSort sort) {
        if (sort == null || sort.field() == null) {
            return Comparator.comparing(ProjectCard::code);
        }
        Comparator<ProjectCard> comparator = switch (sort.field()) {
            case CODE -> Comparator.comparing(ProjectCard::code);
            case TITLE -> Comparator.comparing(ProjectCard::title);
            case REQUESTED_BUDGET -> Comparator.comparing(card -> new BigDecimal(card.requestedBudget()));
            case STATUS -> Comparator.comparing(card -> card.status().name());
        };
        if (sort.direction() == SortDirection.DESC) {
            return comparator.reversed();
        }
        return comparator;
    }

    private static UUID parseId(String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidProjectIdException();
        }
    }
}
