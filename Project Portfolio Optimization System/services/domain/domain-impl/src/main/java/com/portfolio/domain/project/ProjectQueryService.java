package com.portfolio.domain.project;

import com.portfolio.domain.api.project.dto.DepartmentBudgetStatResponse;
import com.portfolio.domain.api.project.dto.DepartmentSummaryResponse;
import com.portfolio.domain.api.project.dto.GoalContributionResponse;
import com.portfolio.domain.api.project.dto.ProjectCardResponse;
import com.portfolio.domain.api.project.dto.ProjectListQuery;
import com.portfolio.domain.api.project.dto.ProjectRegistryStatsResponse;
import com.portfolio.domain.api.project.dto.StatusCountResponse;
import com.portfolio.domain.api.project.dto.StrategicGoalSummaryResponse;
import com.portfolio.domain.error.DomainException;
import com.portfolio.domain.project.entity.ProjectEntity;
import com.portfolio.domain.project.entity.ProjectGoalEntity;
import com.portfolio.domain.reference.StrategicGoalRepository;
import com.portfolio.domain.reference.entity.DepartmentEntity;
import com.portfolio.domain.reference.entity.StrategicGoalEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService {

    private final ProjectRepository projects;
    private final ProjectGoalRepository goals;
    private final StrategicGoalRepository strategicGoals;

    public List<ProjectCardResponse> list(String status) {
        return list(ProjectListQuery.ofStatus(status));
    }

    public List<ProjectCardResponse> list(ProjectListQuery query) {
        return toCards(sort(loadFiltered(query), query));
    }

    public ProjectRegistryStatsResponse stats(ProjectListQuery query) {
        List<ProjectEntity> found = loadFiltered(query);
        Map<String, Long> byStatus = found.stream()
                .collect(Collectors.groupingBy(ProjectEntity::getStatus, TreeMap::new, Collectors.counting()));
        Map<String, BigDecimal> budgetByDepartment = found.stream()
                .collect(Collectors.groupingBy(
                        project -> project.getDepartment().getCode(),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, ProjectEntity::getRequestedBudget, BigDecimal::add)));
        BigDecimal totalRequestedBudget = found.stream()
                .map(ProjectEntity::getRequestedBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProjectRegistryStatsResponse(
                found.size(),
                byStatus.entrySet().stream()
                        .map(entry -> new StatusCountResponse(entry.getKey(), entry.getValue()))
                        .toList(),
                totalRequestedBudget,
                budgetByDepartment.entrySet().stream()
                        .map(entry -> new DepartmentBudgetStatResponse(entry.getKey(), entry.getValue()))
                        .toList());
    }

    public ProjectCardResponse get(UUID id) {
        ProjectEntity project = projects
                .findWithDepartmentById(id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found"));
        return toCards(List.of(project)).getFirst();
    }

    private List<ProjectEntity> loadFiltered(ProjectListQuery query) {
        ProjectListQuery effective = query == null ? ProjectListQuery.ofStatus(null) : query;
        Set<String> statusFilter = resolveStatuses(effective);
        boolean excludeArchived = effective.excludeArchived() == null || Boolean.TRUE.equals(effective.excludeArchived());
        if (statusFilter.contains(ProjectStatuses.ARCHIVED)) {
            excludeArchived = false;
        }
        Stream<ProjectEntity> stream = projects.findAllWithDepartment().stream();
        if (!statusFilter.isEmpty()) {
            stream = stream.filter(project -> statusFilter.contains(project.getStatus()));
        }
        if (excludeArchived) {
            stream = stream.filter(project -> !ProjectStatuses.ARCHIVED.equals(project.getStatus()));
        }
        if (effective.departmentCode() != null && !effective.departmentCode().isBlank()) {
            String departmentCode = effective.departmentCode().trim();
            stream = stream.filter(project -> departmentCode.equalsIgnoreCase(project.getDepartment().getCode()));
        }
        if (effective.minBudget() != null) {
            stream = stream.filter(project -> project.getRequestedBudget().compareTo(effective.minBudget()) >= 0);
        }
        if (effective.maxBudget() != null) {
            stream = stream.filter(project -> project.getRequestedBudget().compareTo(effective.maxBudget()) <= 0);
        }
        List<ProjectEntity> filtered = stream.toList();
        return filterByGoalCode(filtered, effective.goalCode());
    }

    private List<ProjectEntity> filterByGoalCode(List<ProjectEntity> found, String goalCode) {
        if (goalCode == null || goalCode.isBlank() || found.isEmpty()) {
            return found;
        }
        String normalized = goalCode.trim();
        List<UUID> ids = found.stream().map(ProjectEntity::getId).toList();
        List<ProjectGoalEntity> links = goals.findByProjectIds(ids);
        Map<UUID, StrategicGoalEntity> goalsById = strategicGoals
                .findAllById(links.stream().map(link -> link.getId().getStrategicGoalId()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(StrategicGoalEntity::getId, Function.identity()));
        Set<UUID> matching = links.stream()
                .filter(link -> {
                    StrategicGoalEntity goal = goalsById.get(link.getId().getStrategicGoalId());
                    return goal != null && normalized.equalsIgnoreCase(goal.getCode());
                })
                .map(link -> link.getId().getProjectId())
                .collect(Collectors.toSet());
        return found.stream().filter(project -> matching.contains(project.getId())).toList();
    }

    private static List<ProjectEntity> sort(List<ProjectEntity> found, ProjectListQuery query) {
        String field = query == null || query.sort() == null || query.sort().isBlank()
                ? "CODE"
                : query.sort().trim().toUpperCase();
        if (!ProjectStatuses.SORT_FIELDS.contains(field)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Unknown sort field");
        }
        String direction = query == null || query.direction() == null || query.direction().isBlank()
                ? "ASC"
                : query.direction().trim().toUpperCase();
        if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Unknown sort direction");
        }
        Comparator<ProjectEntity> comparator = switch (field) {
            case "TITLE" -> Comparator.comparing(ProjectEntity::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "REQUESTED_BUDGET" -> Comparator.comparing(ProjectEntity::getRequestedBudget);
            case "STATUS" -> Comparator.comparing(ProjectEntity::getStatus, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(ProjectEntity::getCode, String.CASE_INSENSITIVE_ORDER);
        };
        if ("DESC".equals(direction)) {
            comparator = comparator.reversed();
        }
        return found.stream().sorted(comparator).toList();
    }

    private static Set<String> resolveStatuses(ProjectListQuery query) {
        List<String> raw = new ArrayList<>();
        if (query.status() != null && !query.status().isBlank()) {
            raw.add(query.status());
        }
        if (query.statuses() != null) {
            raw.addAll(query.statuses());
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String status : raw) {
            if (status != null && !status.isBlank()) {
                normalized.add(normalizeStatus(status));
            }
        }
        return normalized;
    }

    private static String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!ProjectStatuses.ALL.contains(normalized)) {
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
