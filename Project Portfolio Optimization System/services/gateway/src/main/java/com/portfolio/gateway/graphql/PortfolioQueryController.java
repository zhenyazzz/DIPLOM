package com.portfolio.gateway.graphql;

import com.portfolio.gateway.catalog.model.ProjectCard;
import com.portfolio.gateway.catalog.model.ProjectRegistryStats;
import com.portfolio.gateway.catalog.query.ProjectFilter;
import com.portfolio.gateway.catalog.query.ProjectSort;
import com.portfolio.gateway.catalog.service.PortfolioQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PortfolioQueryController {

    private final PortfolioQueryService queries;

    @QueryMapping
    public ProjectCard project(@Argument String id) {
        return queries.project(id);
    }

    @QueryMapping
    public List<ProjectCard> projects(@Argument ProjectFilter filter, @Argument ProjectSort sort) {
        return queries.projects(filter, sort);
    }

    @QueryMapping
    public ProjectRegistryStats projectRegistryStats(@Argument ProjectFilter filter) {
        return queries.projectRegistryStats(filter);
    }
}
