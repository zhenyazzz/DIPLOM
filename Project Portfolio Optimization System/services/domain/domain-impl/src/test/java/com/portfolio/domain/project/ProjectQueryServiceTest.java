package com.portfolio.domain.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.portfolio.domain.api.project.dto.ProjectCardResponse;
import com.portfolio.domain.api.project.dto.ProjectListQuery;
import com.portfolio.domain.api.project.dto.ProjectRegistryStatsResponse;
import com.portfolio.domain.error.DomainException;
import com.portfolio.domain.project.entity.ProjectEntity;
import com.portfolio.domain.project.entity.ProjectGoalEntity;
import com.portfolio.domain.project.entity.ProjectGoalId;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ProjectQueryServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-4000-8000-000000000601");
    private static final UUID DEPARTMENT_ID = UUID.fromString("00000000-0000-4000-8000-000000000101");
    private static final UUID GOAL_ID = UUID.fromString("00000000-0000-4000-8000-000000000201");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Mock
    private ProjectRepository projects;

    @Mock
    private ProjectGoalRepository goals;

    @Mock
    private StrategicGoalRepository strategicGoals;

    @InjectMocks
    private ProjectQueryService service;

    @Test
    void getReturnsCardWithDepartmentAndGoals() {
        when(projects.findWithDepartmentById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(goals.findByProjectIds(List.of(PROJECT_ID))).thenReturn(List.of(link()));
        when(strategicGoals.findAllById(Set.of(GOAL_ID))).thenReturn(List.of(goal()));

        ProjectCardResponse card = service.get(PROJECT_ID);

        assertThat(card.code()).isEqualTo("PRJ-PORTAL");
        assertThat(card.department().code()).isEqualTo("IT");
        assertThat(card.ownerId()).isEqualTo(OWNER_ID);
        assertThat(card.goals()).singleElement().satisfies(contribution -> {
            assertThat(contribution.level()).isEqualTo(5);
            assertThat(contribution.goal().code()).isEqualTo("ALIGN");
        });
    }

    @Test
    void getMissingProjectIsNotFound() {
        when(projects.findWithDepartmentById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(PROJECT_ID))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listRejectsUnknownStatus() {
        assertThatThrownBy(() -> service.list("NOPE"))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getCode())
                .isEqualTo("BAD_REQUEST");
    }

    @Test
    void listExcludesArchivedByDefault() {
        when(projects.findAllWithDepartment()).thenReturn(List.of(project(), archived()));
        when(goals.findByProjectIds(any())).thenReturn(List.of());
        when(strategicGoals.findAllById(any())).thenReturn(List.of());

        List<ProjectCardResponse> cards = service.list(new ProjectListQuery(
                null, null, null, null, null, null, true, "CODE", "ASC"));

        assertThat(cards).extracting(ProjectCardResponse::code).containsExactly("PRJ-PORTAL");
    }

    @Test
    void statsAggregatesBudgetAndStatus() {
        when(projects.findAllWithDepartment()).thenReturn(List.of(project(), financeDraft()));

        ProjectRegistryStatsResponse stats = service.stats(new ProjectListQuery(
                null, null, null, null, null, null, true, null, null));

        assertThat(stats.totalCount()).isEqualTo(2);
        assertThat(stats.totalRequestedBudget()).isEqualByComparingTo("2100000.0000");
        assertThat(stats.byStatus()).extracting(item -> item.status() + ":" + item.count())
                .containsExactly("DRAFT:1", "SUBMITTED:1");
        assertThat(stats.budgetByDepartment())
                .extracting(item -> item.departmentCode() + ":" + item.totalRequestedBudget().toPlainString())
                .containsExactly("FIN:900000.0000", "IT:1200000.0000");
    }

    private static ProjectEntity project() {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(DEPARTMENT_ID);
        department.setCode("IT");
        department.setName("Информационные технологии");
        ProjectEntity project = new ProjectEntity();
        project.setId(PROJECT_ID);
        project.setCode("PRJ-PORTAL");
        project.setTitle("Портал самообслуживания");
        project.setStatus("SUBMITTED");
        project.setDepartment(department);
        project.setOwnerId(OWNER_ID);
        project.setRequestedBudget(new BigDecimal("1200000.0000"));
        return project;
    }

    private static ProjectGoalEntity link() {
        ProjectGoalEntity link = new ProjectGoalEntity();
        link.setId(new ProjectGoalId(PROJECT_ID, GOAL_ID));
        link.setContributionLevel((short) 5);
        return link;
    }

    private static StrategicGoalEntity goal() {
        StrategicGoalEntity goal = new StrategicGoalEntity();
        goal.setId(GOAL_ID);
        goal.setCode("ALIGN");
        goal.setName("Стратегическое соответствие");
        return goal;
    }

    private static ProjectEntity archived() {
        ProjectEntity project = project();
        project.setId(UUID.fromString("00000000-0000-4000-8000-000000000699"));
        project.setCode("PRJ-OLD");
        project.setStatus("ARCHIVED");
        return project;
    }

    private static ProjectEntity financeDraft() {
        DepartmentEntity finance = new DepartmentEntity();
        finance.setId(UUID.fromString("00000000-0000-4000-8000-000000000102"));
        finance.setCode("FIN");
        finance.setName("Финансы");
        ProjectEntity project = new ProjectEntity();
        project.setId(UUID.fromString("00000000-0000-4000-8000-000000000602"));
        project.setCode("PRJ-LEDGER");
        project.setTitle("Обновление учётной системы");
        project.setStatus("DRAFT");
        project.setDepartment(finance);
        project.setOwnerId(OWNER_ID);
        project.setRequestedBudget(new BigDecimal("900000.0000"));
        return project;
    }
}
