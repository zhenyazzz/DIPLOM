package com.portfolio.domain.project.web;

import com.portfolio.domain.api.project.ProjectCardResponse;
import com.portfolio.domain.project.ProjectQueryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectQueryController {

    private final ProjectQueryService projects;

    @GetMapping
    public List<ProjectCardResponse> list(@RequestParam(name = "status", required = false) String status) {
        return projects.list(status);
    }

    @GetMapping("/{id}")
    public ProjectCardResponse get(@PathVariable UUID id) {
        return projects.get(id);
    }
}
