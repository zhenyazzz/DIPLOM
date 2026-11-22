package com.portfolio.domain.project;

import com.portfolio.domain.api.project.dto.GoalContributionInput;
import com.portfolio.domain.api.project.dto.ProjectCardResponse;
import com.portfolio.domain.api.project.dto.RegisterProjectRequest;
import com.portfolio.domain.error.DomainException;
import com.portfolio.domain.project.entity.ProjectEntity;
import com.portfolio.domain.project.entity.ProjectGoalEntity;
import com.portfolio.domain.project.entity.ProjectGoalId;
import com.portfolio.domain.reference.DepartmentRepository;
import com.portfolio.domain.reference.StrategicGoalRepository;
import com.portfolio.domain.reference.entity.DepartmentEntity;
import com.portfolio.domain.reference.entity.StrategicGoalEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectLifecycleService {

    private static final Set<String> SUBMIT_FROM = Set.of(ProjectStatuses.DRAFT, ProjectStatuses.RETURNED_FOR_REVISION);
    private static final Set<String> START_REVIEW_FROM = Set.of(ProjectStatuses.SUBMITTED);
    private static final Set<String> COMPLETE_EVALUATION_FROM = Set.of(ProjectStatuses.UNDER_REVIEW);
    private static final Set<String> APPROVE_FROM = Set.of(ProjectStatuses.EVALUATED);
    private static final Set<String> RETURN_OR_REJECT_FROM =
            Set.of(ProjectStatuses.SUBMITTED, ProjectStatuses.UNDER_REVIEW, ProjectStatuses.EVALUATED);
    private static final Set<String> ARCHIVE_FROM =
            Set.of(ProjectStatuses.DRAFT, ProjectStatuses.REJECTED, ProjectStatuses.RETURNED_FOR_REVISION);

    private final ProjectRepository projects;
    private final ProjectGoalRepository goals;
    private final DepartmentRepository departments;
    private final StrategicGoalRepository strategicGoals;
    private final ProjectQueryService queries;

    public ProjectCardResponse register(RegisterProjectRequest request) {
        validateRegister(request);
        DepartmentEntity department = departments
                .findById(request.departmentId())
                .orElseThrow(() -> badRequest("UNKNOWN_DEPARTMENT", "Department not found"));
        Set<UUID> goalIds = request.goals().stream()
                .map(GoalContributionInput::goalId)
                .collect(Collectors.toCollection(HashSet::new));
        if (goalIds.size() != request.goals().size()) {
            throw badRequest("DUPLICATE_GOAL", "Strategic goals must be unique for a project");
        }
        List<StrategicGoalEntity> foundGoals = strategicGoals.findAllById(goalIds);
        if (foundGoals.size() != goalIds.size()) {
            throw badRequest("UNKNOWN_STRATEGIC_GOAL", "Strategic goal not found");
        }

        UUID id = UUID.randomUUID();
        ProjectEntity project = new ProjectEntity();
        project.setId(id);
        project.setCode(request.code().trim());
        project.setTitle(request.title().trim());
        project.setDescription(request.description());
        project.setStatus(ProjectStatuses.DRAFT);
        project.setDepartment(department);
        project.setOwnerId(request.ownerId());
        project.setRequestedBudget(request.requestedBudget());
        projects.save(project);

        List<ProjectGoalEntity> links = request.goals().stream()
                .map(input -> {
                    ProjectGoalEntity link = new ProjectGoalEntity();
                    link.setId(new ProjectGoalId(id, input.goalId()));
                    link.setContributionLevel((short) input.level());
                    return link;
                })
                .toList();
        goals.saveAll(links);
        return queries.get(id);
    }

    public ProjectCardResponse submit(UUID id) {
        return transition(id, SUBMIT_FROM, ProjectStatuses.SUBMITTED, project -> project.setSubmittedAt(Instant.now()));
    }

    public ProjectCardResponse startReview(UUID id) {
        return transition(id, START_REVIEW_FROM, ProjectStatuses.UNDER_REVIEW, null);
    }

    public ProjectCardResponse completeEvaluation(UUID id) {
        return transition(id, COMPLETE_EVALUATION_FROM, ProjectStatuses.EVALUATED, null);
    }

    public ProjectCardResponse approveForPortfolio(UUID id) {
        return transition(id, APPROVE_FROM, ProjectStatuses.APPROVED_FOR_PORTFOLIO, null);
    }

    public ProjectCardResponse returnForRevision(UUID id, String reason) {
        return transition(id, RETURN_OR_REJECT_FROM, ProjectStatuses.RETURNED_FOR_REVISION, null);
    }

    public ProjectCardResponse reject(UUID id, String reason) {
        return transition(id, RETURN_OR_REJECT_FROM, ProjectStatuses.REJECTED, null);
    }

    public ProjectCardResponse archive(UUID id) {
        return transition(id, ARCHIVE_FROM, ProjectStatuses.ARCHIVED, project -> project.setArchivedAt(Instant.now()));
    }

    private ProjectCardResponse transition(
            UUID id, Set<String> allowedFrom, String target, Consumer<ProjectEntity> extra) {
        ProjectEntity project = projects
                .findById(id)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found"));
        if (!allowedFrom.contains(project.getStatus())) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "ILLEGAL_STATUS_TRANSITION",
                    "Cannot change status from " + project.getStatus() + " to " + target);
        }
        project.setStatus(target);
        if (extra != null) {
            extra.accept(project);
        }
        projects.save(project);
        return queries.get(id);
    }

    private void validateRegister(RegisterProjectRequest request) {
        if (request == null) {
            throw badRequest("BAD_REQUEST", "Request is required");
        }
        String code = requireText(request.code(), "CODE_REQUIRED", "Project code is required");
        if (projects.existsByCode(code)) {
            throw new DomainException(HttpStatus.CONFLICT, "DUPLICATE_CODE", "Project code already exists");
        }
        requireText(request.title(), "TITLE_REQUIRED", "Project title is required");
        if (request.ownerId() == null) {
            throw badRequest("OWNER_REQUIRED", "Owner is required");
        }
        if (request.requestedBudget() == null || request.requestedBudget().compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("INVALID_BUDGET", "Requested budget must be greater than or equal to 0");
        }
        if (request.departmentId() == null) {
            throw badRequest("UNKNOWN_DEPARTMENT", "Department is required");
        }
        if (request.goals() == null || request.goals().isEmpty()) {
            throw badRequest("GOALS_REQUIRED", "At least one strategic goal is required");
        }
        for (GoalContributionInput goal : request.goals()) {
            if (goal == null || goal.goalId() == null) {
                throw badRequest("UNKNOWN_STRATEGIC_GOAL", "Strategic goal is required");
            }
            if (goal.level() < 1 || goal.level() > 5) {
                throw badRequest("INVALID_CONTRIBUTION_LEVEL", "Contribution level must be between 1 and 5");
            }
        }
    }

    private static String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw badRequest(code, message);
        }
        return value.trim();
    }

    private static DomainException badRequest(String code, String message) {
        return new DomainException(HttpStatus.BAD_REQUEST, code, message);
    }
}
