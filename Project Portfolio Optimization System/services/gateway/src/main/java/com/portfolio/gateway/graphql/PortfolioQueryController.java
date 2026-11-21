package com.portfolio.gateway.graphql;

import com.portfolio.gateway.catalog.PortfolioQueryService;
import com.portfolio.gateway.catalog.ProjectCard;
import com.portfolio.gateway.catalog.ProjectFilter;
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
    public List<ProjectCard> projects(@Argument ProjectFilter filter) {
        return queries.projects(filter);
    }
}
