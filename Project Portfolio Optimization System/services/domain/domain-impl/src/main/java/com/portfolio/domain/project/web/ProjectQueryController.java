package com.portfolio.domain.project.web;

import com.portfolio.domain.api.project.ProjectQueryApi;
import com.portfolio.domain.api.project.dto.ProjectCardResponse;
import com.portfolio.domain.api.project.dto.ProjectListQuery;
import com.portfolio.domain.api.project.dto.ProjectRegistryStatsResponse;
import com.portfolio.domain.project.ProjectQueryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProjectQueryController implements ProjectQueryApi {

    private final ProjectQueryService projects;

    @Override
    public List<ProjectCardResponse> list(
            String status,
            List<String> statuses,
            String departmentCode,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            String goalCode,
            Boolean excludeArchived,
            String sort,
            String direction) {
        return projects.list(query(
                status, statuses, departmentCode, minBudget, maxBudget, goalCode, excludeArchived, sort, direction));
    }

    @Override
    public ProjectRegistryStatsResponse stats(
            String status,
            List<String> statuses,
            String departmentCode,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            String goalCode,
            Boolean excludeArchived) {
        return projects.stats(query(
                status, statuses, departmentCode, minBudget, maxBudget, goalCode, excludeArchived, null, null));
    }

    @Override
    public ProjectCardResponse get(UUID id) {
        return projects.get(id);
    }

    private static ProjectListQuery query(
            String status,
            List<String> statuses,
            String departmentCode,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            String goalCode,
            Boolean excludeArchived,
            String sort,
            String direction) {
        return new ProjectListQuery(
                status,
                statuses,
                departmentCode,
                minBudget,
                maxBudget,
                goalCode,
                excludeArchived,
                sort,
                direction);
    }
}
