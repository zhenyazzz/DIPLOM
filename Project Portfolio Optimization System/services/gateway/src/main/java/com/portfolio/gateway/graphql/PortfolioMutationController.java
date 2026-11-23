package com.portfolio.gateway.graphql;

import com.portfolio.gateway.catalog.command.RegisterInitiativeCommand;
import com.portfolio.gateway.catalog.model.ProjectCard;
import com.portfolio.gateway.catalog.service.PortfolioCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PortfolioMutationController {

    private final PortfolioCommandService commands;

    @MutationMapping
    public ProjectCard registerInitiative(@Argument RegisterInitiativeCommand input) {
        return commands.registerInitiative(input);
    }

    @MutationMapping
    public ProjectCard submitInitiative(@Argument String id) {
        return commands.submitInitiative(id);
    }

    @MutationMapping
    public ProjectCard startReview(@Argument String id) {
        return commands.startReview(id);
    }

    @MutationMapping
    public ProjectCard completeEvaluation(@Argument String id) {
        return commands.completeEvaluation(id);
    }

    @MutationMapping
    public ProjectCard approveForPortfolio(@Argument String id) {
        return commands.approveForPortfolio(id);
    }

    @MutationMapping
    public ProjectCard returnForRevision(@Argument String id, @Argument String reason) {
        return commands.returnForRevision(id, reason);
    }

    @MutationMapping
    public ProjectCard rejectInitiative(@Argument String id, @Argument String reason) {
        return commands.rejectInitiative(id, reason);
    }

    @MutationMapping
    public ProjectCard archiveInitiative(@Argument String id) {
        return commands.archiveInitiative(id);
    }
}
