package com.portfolio.gateway.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioCommandService {

    private final DomainProjectCatalog projects;
    private final ParticipantDirectory participants;

    public ProjectCard registerInitiative(RegisterInitiativeCommand input) {
        parseId(input.ownerId());
        return toCard(projects.register(input));
    }

    public ProjectCard submitInitiative(String rawId) {
        return toCard(projects.submit(parseId(rawId)));
    }

    public ProjectCard startReview(String rawId) {
        return toCard(projects.startReview(parseId(rawId)));
    }

    public ProjectCard completeEvaluation(String rawId) {
        return toCard(projects.completeEvaluation(parseId(rawId)));
    }

    public ProjectCard approveForPortfolio(String rawId) {
        return toCard(projects.approve(parseId(rawId)));
    }

    public ProjectCard returnForRevision(String rawId, String reason) {
        return toCard(projects.returnForRevision(parseId(rawId), reason));
    }

    public ProjectCard rejectInitiative(String rawId, String reason) {
        return toCard(projects.reject(parseId(rawId), reason));
    }

    public ProjectCard archiveInitiative(String rawId) {
        return toCard(projects.archive(parseId(rawId)));
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
