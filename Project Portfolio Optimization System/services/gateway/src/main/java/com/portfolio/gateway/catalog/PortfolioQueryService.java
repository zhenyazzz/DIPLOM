package com.portfolio.gateway.catalog;

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

    public List<ProjectCard> projects(ProjectFilter filter) {
        ProjectStatus status = filter == null ? null : filter.status();
        return projects.findAll(status).stream()
                .map(this::toCard)
                .sorted(Comparator.comparing(ProjectCard::code))
                .toList();
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

    private static UUID parseId(String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidProjectIdException();
        }
    }
}
