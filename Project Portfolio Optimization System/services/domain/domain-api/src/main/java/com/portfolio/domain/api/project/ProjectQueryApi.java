package com.portfolio.domain.api.project;

import com.portfolio.domain.api.project.dto.ProjectCardResponse;
import com.portfolio.domain.api.project.dto.ProjectRegistryStatsResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Read contract for initiative cards ({@code /api/v1/projects}).
 */
@RequestMapping("/api/v1/projects")
public interface ProjectQueryApi {

    @GetMapping
    List<ProjectCardResponse> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @RequestParam(name = "departmentCode", required = false) String departmentCode,
            @RequestParam(name = "minBudget", required = false) BigDecimal minBudget,
            @RequestParam(name = "maxBudget", required = false) BigDecimal maxBudget,
            @RequestParam(name = "goalCode", required = false) String goalCode,
            @RequestParam(name = "excludeArchived", required = false, defaultValue = "true") Boolean excludeArchived,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", required = false) String direction);

    @GetMapping("/stats")
    ProjectRegistryStatsResponse stats(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @RequestParam(name = "departmentCode", required = false) String departmentCode,
            @RequestParam(name = "minBudget", required = false) BigDecimal minBudget,
            @RequestParam(name = "maxBudget", required = false) BigDecimal maxBudget,
            @RequestParam(name = "goalCode", required = false) String goalCode,
            @RequestParam(name = "excludeArchived", required = false, defaultValue = "true") Boolean excludeArchived);

    @GetMapping("/{id}")
    ProjectCardResponse get(@PathVariable UUID id);
}
