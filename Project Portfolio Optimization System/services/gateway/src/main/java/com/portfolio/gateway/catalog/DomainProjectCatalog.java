package com.portfolio.gateway.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DomainProjectCatalog {

    Optional<ProjectSnapshot> findById(UUID id);

    List<ProjectSnapshot> findAll(ProjectStatus status);
}
