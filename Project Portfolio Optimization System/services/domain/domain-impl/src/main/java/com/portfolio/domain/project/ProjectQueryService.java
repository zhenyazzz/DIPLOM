package com.portfolio.domain.project;

import com.portfolio.domain.api.project.DepartmentSummaryResponse;
import com.portfolio.domain.api.project.GoalContributionResponse;
import com.portfolio.domain.api.project.ProjectCardResponse;
import com.portfolio.domain.api.project.StrategicGoalSummaryResponse;
import com.portfolio.domain.error.DomainException;
import com.portfolio.domain.project.entity.ProjectEntity;
import com.portfolio.domain.project.entity.ProjectGoalEntity;
import com.portfolio.domain.reference.StrategicGoalRepository;
import com.portfolio.domain.reference.entity.DepartmentEntity;
import com.portfolio.domain.reference.entity.StrategicGoalEntity;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService {

    private static final Set<String> STATUSES = Set.of(
            "DRAFT",
            "SUBMITTED",
            "UNDER_REVIEW",
            "EVALUATED",
            "APPROVED_FOR_PORTFOLIO",
            "REJECTED",
            "RETURNED_FOR_REVISION",
            "ARCHIVED");

    private final ProjectRepository projects;
    private final ProjectGoalRepository goals;
    private final StrategicGoalRepository strategicGoals;

    public List<ProjectCardResponse> list(String status) {
        List<ProjectEntity> found = status == null || status.isBlank()
                ? projects.findAllWithDepartment()
                : projects.findByStatusWithDepartment(normalizeStatus(status));
        return toCards(found);
    }

    public ProjectCardResponse get(UUID id) {
        ProjectEntity project = projects
                .findWithDepartmentById(id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found"));
        return toCards(List.of(project)).getFirst();
    }

    private static String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Unknown project status");
        }
        return normalized;
    }

    private List<ProjectCardResponse> toCards(List<ProjectEntity> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = found.stream().map(ProjectEntity::getId).toList();
        List<ProjectGoalEntity> links = goals.findByProjectIds(ids);
        Map<UUID, StrategicGoalEntity> goalsById = strategicGoals
                .findAllById(links.stream().map(link -> link.getId().getStrategicGoalId()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(StrategicGoalEntity::getId, Function.identity()));
        Map<UUID, List<ProjectGoalEntity>> linksByProject =
                links.stream().collect(Collectors.groupingBy(link -> link.getId().getProjectId()));
        return found.stream()
                .map(project -> toCard(project, linksByProject.getOrDefault(project.getId(), List.of()), goalsById))
                .toList();
    }

    private static ProjectCardResponse toCard(
            ProjectEntity project, List<ProjectGoalEntity> links, Map<UUID, StrategicGoalEntity> goalsById) {
        DepartmentEntity department = project.getDepartment();
        List<GoalContributionResponse> contributions = links.stream()
                .map(link -> {
                    StrategicGoalEntity goal = goalsById.get(link.getId().getStrategicGoalId());
                    return new GoalContributionResponse(
                            new StrategicGoalSummaryResponse(goal.getId(), goal.getCode(), goal.getName()),
                            link.getContributionLevel());
                })
                .sorted(Comparator.comparing(item -> item.goal().code()))
                .toList();
        return new ProjectCardResponse(
                project.getId(),
                project.getCode(),
                project.getTitle(),
                project.getDescription(),
                project.getStatus(),
                project.getRequestedBudget(),
                new DepartmentSummaryResponse(department.getId(), department.getCode(), department.getName()),
                project.getOwnerId(),
                contributions);
    }
}
