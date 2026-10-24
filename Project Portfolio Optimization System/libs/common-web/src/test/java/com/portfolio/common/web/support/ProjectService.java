package com.portfolio.common.web.support;

import com.portfolio.common.web.config.CommonWebProperties;
import com.portfolio.common.web.crud.AbstractJpaCrudService;
import com.portfolio.common.web.filter.FieldAllowList;
import com.portfolio.common.web.filter.FieldRule;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ProjectService
        extends AbstractJpaCrudService<ProjectEntity, CreateProjectRequest, UpdateProjectRequest, ProjectResponse> {

    public ProjectService(ProjectRepository repository, ProjectMapper mapper, CommonWebProperties properties) {
        super(repository, mapper, projectFields(), properties);
    }

    private static FieldAllowList projectFields() {
        return FieldAllowList.of(
                FieldRule.filterAndSort("name", String.class),
                FieldRule.filterAndSort("status", ProjectStatus.class),
                FieldRule.filterAndSort("budget", BigDecimal.class)
        );
    }
}
