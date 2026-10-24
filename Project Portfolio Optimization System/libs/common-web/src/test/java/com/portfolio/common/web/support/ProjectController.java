package com.portfolio.common.web.support;

import com.portfolio.common.web.web.AbstractCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController
        extends AbstractCrudController<CreateProjectRequest, UpdateProjectRequest, ProjectResponse> {

    public ProjectController(ProjectService service) {
        super(service);
    }
}
