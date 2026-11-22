package com.portfolio.domain.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.domain.api.project.dto.DepartmentSummaryResponse;
import com.portfolio.domain.api.project.dto.GoalContributionInput;
import com.portfolio.domain.api.project.dto.ProjectCardResponse;
import com.portfolio.domain.api.project.dto.RegisterProjectRequest;
import com.portfolio.domain.error.DomainException;
import com.portfolio.domain.project.entity.ProjectEntity;
import com.portfolio.domain.project.entity.ProjectGoalEntity;
import com.portfolio.domain.reference.DepartmentRepository;
import com.portfolio.domain.reference.StrategicGoalRepository;
import com.portfolio.domain.reference.entity.DepartmentEntity;
import com.portfolio.domain.reference.entity.StrategicGoalEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ProjectLifecycleServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-4000-8000-000000000601");
    private static final UUID DEPARTMENT_ID = UUID.fromString("00000000-0000-4000-8000-000000000101");
    private static final UUID GOAL_ID = UUID.fromString("00000000-0000-4000-8000-000000000201");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Mock
    private ProjectRepository projects;

    @Mock
    private ProjectGoalRepository goals;

    @Mock
    private DepartmentRepository departments;

    @Mock
    private StrategicGoalRepository strategicGoals;

    @Mock
    private ProjectQueryService queries;

    @InjectMocks
    private ProjectLifecycleService service;

    @Test
    void happyPathDraftToApproved() {
        ProjectEntity project = project(ProjectStatuses.DRAFT);
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projects.save(project)).thenReturn(project);
        when(queries.get(PROJECT_ID)).thenReturn(card(ProjectStatuses.APPROVED_FOR_PORTFOLIO));

        service.submit(PROJECT_ID);
        assertThat(project.getStatus()).isEqualTo(ProjectStatuses.SUBMITTED);
        assertThat(project.getSubmittedAt()).isNotNull();

        service.startReview(PROJECT_ID);
        assertThat(project.getStatus()).isEqualTo(ProjectStatuses.UNDER_REVIEW);

        service.completeEvaluation(PROJECT_ID);
        assertThat(project.getStatus()).isEqualTo(ProjectStatuses.EVALUATED);

        ProjectCardResponse card = service.approveForPortfolio(PROJECT_ID);
        assertThat(project.getStatus()).isEqualTo(ProjectStatuses.APPROVED_FOR_PORTFOLIO);
        assertThat(card.status()).isEqualTo(ProjectStatuses.APPROVED_FOR_PORTFOLIO);
    }

    @Test
    void illegalTransitionThrows() {
        ProjectEntity project = project(ProjectStatuses.DRAFT);
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.startReview(PROJECT_ID))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException error = (DomainException) ex;
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(error.getCode()).isEqualTo("ILLEGAL_STATUS_TRANSITION");
                });
        assertThat(project.getStatus()).isEqualTo(ProjectStatuses.DRAFT);
    }

    @Test
    void registerRejectsMissingGoals() {
        when(projects.existsByCode("PRJ-NEW")).thenReturn(false);

        RegisterProjectRequest request = new RegisterProjectRequest(
                "PRJ-NEW",
                "New portal",
                null,
                DEPARTMENT_ID,
                OWNER_ID,
                new BigDecimal("10.00"),
                List.of());

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getCode())
                .isEqualTo("GOALS_REQUIRED");
    }

    @Test
    void registerRejectsNegativeBudget() {
        when(projects.existsByCode("PRJ-NEW")).thenReturn(false);

        RegisterProjectRequest request = new RegisterProjectRequest(
                "PRJ-NEW",
                "New portal",
                null,
                DEPARTMENT_ID,
                OWNER_ID,
                new BigDecimal("-1"),
                List.of(new GoalContributionInput(GOAL_ID, 3)));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getCode())
                .isEqualTo("INVALID_BUDGET");
    }

    @Test
    void registerRejectsUnknownDepartment() {
        when(projects.existsByCode("PRJ-NEW")).thenReturn(false);
        when(departments.findById(DEPARTMENT_ID)).thenReturn(Optional.empty());

        RegisterProjectRequest request = new RegisterProjectRequest(
                "PRJ-NEW",
                "New portal",
                null,
                DEPARTMENT_ID,
                OWNER_ID,
                new BigDecimal("10.00"),
                List.of(new GoalContributionInput(GOAL_ID, 3)));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getCode())
                .isEqualTo("UNKNOWN_DEPARTMENT");
    }

    @Test
    void archiveSetsArchivedAt() {
        ProjectEntity project = project(ProjectStatuses.DRAFT);
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projects.save(project)).thenReturn(project);
        when(queries.get(PROJECT_ID)).thenReturn(card(ProjectStatuses.ARCHIVED));

        service.archive(PROJECT_ID);

        assertThat(project.getStatus()).isEqualTo(ProjectStatuses.ARCHIVED);
        assertThat(project.getArchivedAt()).isNotNull();
    }

    @Test
    void registerCreatesDraftWithGoals() {
        when(projects.existsByCode("PRJ-NEW")).thenReturn(false);
        when(departments.findById(DEPARTMENT_ID)).thenReturn(Optional.of(department()));
        when(strategicGoals.findAllById(Set.of(GOAL_ID))).thenReturn(List.of(goal()));
        when(projects.save(any(ProjectEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queries.get(any(UUID.class))).thenReturn(card(ProjectStatuses.DRAFT));

        RegisterProjectRequest request = new RegisterProjectRequest(
                "PRJ-NEW",
                "New portal",
                "desc",
                DEPARTMENT_ID,
                OWNER_ID,
                new BigDecimal("10.00"),
                List.of(new GoalContributionInput(GOAL_ID, 4)));

        service.register(request);

        ArgumentCaptor<ProjectEntity> projectCaptor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projects).save(projectCaptor.capture());
        ProjectEntity saved = projectCaptor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatuses.DRAFT);
        assertThat(saved.getOwnerId()).isEqualTo(OWNER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProjectGoalEntity>> linksCaptor = ArgumentCaptor.forClass(List.class);
        verify(goals).saveAll(linksCaptor.capture());
        assertThat(linksCaptor.getValue()).singleElement().satisfies(link -> {
            assertThat(link.getId().getProjectId()).isEqualTo(saved.getId());
            assertThat(link.getId().getStrategicGoalId()).isEqualTo(GOAL_ID);
            assertThat(link.getContributionLevel()).isEqualTo((short) 4);
        });
    }

    @Test
    void missingProjectIsNotFound() {
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(PROJECT_ID))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static ProjectEntity project(String status) {
        ProjectEntity project = new ProjectEntity();
        project.setId(PROJECT_ID);
        project.setCode("PRJ-PORTAL");
        project.setTitle("Портал самообслуживания");
        project.setStatus(status);
        project.setDepartment(department());
        project.setOwnerId(OWNER_ID);
        project.setRequestedBudget(new BigDecimal("1200000.0000"));
        project.setVersion(0L);
        return project;
    }

    private static DepartmentEntity department() {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(DEPARTMENT_ID);
        department.setCode("IT");
        department.setName("Информационные технологии");
        return department;
    }

    private static StrategicGoalEntity goal() {
        StrategicGoalEntity goal = new StrategicGoalEntity();
        goal.setId(GOAL_ID);
        goal.setCode("ALIGN");
        goal.setName("Стратегическое соответствие");
        return goal;
    }

    private static ProjectCardResponse card(String status) {
        return new ProjectCardResponse(
                PROJECT_ID,
                "PRJ-PORTAL",
                "Портал самообслуживания",
                null,
                status,
                new BigDecimal("1200000.0000"),
                new DepartmentSummaryResponse(DEPARTMENT_ID, "IT", "Информационные технологии"),
                OWNER_ID,
                List.of());
    }
}
