package com.portfolio.gateway.catalog.port;

import com.portfolio.gateway.catalog.command.RegisterInitiativeCommand;
import com.portfolio.gateway.catalog.model.ProjectRegistryStats;
import com.portfolio.gateway.catalog.model.ProjectSnapshot;
import com.portfolio.gateway.catalog.query.ProjectFilter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DomainProjectCatalog {

    Optional<ProjectSnapshot> findById(UUID id);

    List<ProjectSnapshot> findAll(ProjectFilter filter);

    ProjectSnapshot register(RegisterInitiativeCommand command);

    ProjectSnapshot submit(UUID id);

    ProjectSnapshot startReview(UUID id);

    ProjectSnapshot completeEvaluation(UUID id);

    ProjectSnapshot approve(UUID id);

    ProjectSnapshot returnForRevision(UUID id, String reason);

    ProjectSnapshot reject(UUID id, String reason);

    ProjectSnapshot archive(UUID id);

    ProjectRegistryStats stats(ProjectFilter filter);
}
