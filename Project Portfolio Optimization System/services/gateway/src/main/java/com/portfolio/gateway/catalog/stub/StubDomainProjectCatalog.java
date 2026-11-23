package com.portfolio.gateway.catalog.stub;

import com.portfolio.gateway.catalog.command.GoalContributionInput;
import com.portfolio.gateway.catalog.command.RegisterInitiativeCommand;
import com.portfolio.gateway.catalog.exception.CatalogValidationException;
import com.portfolio.gateway.catalog.exception.ConflictException;
import com.portfolio.gateway.catalog.exception.IllegalStatusTransitionException;
import com.portfolio.gateway.catalog.exception.InvalidProjectIdException;
import com.portfolio.gateway.catalog.exception.ProjectNotFoundException;
import com.portfolio.gateway.catalog.model.DepartmentBudgetStat;
import com.portfolio.gateway.catalog.model.DepartmentCard;
import com.portfolio.gateway.catalog.model.GoalContributionCard;
import com.portfolio.gateway.catalog.model.ProjectRegistryStats;
import com.portfolio.gateway.catalog.model.ProjectSnapshot;
import com.portfolio.gateway.catalog.model.ProjectStatus;
import com.portfolio.gateway.catalog.model.StatusCount;
import com.portfolio.gateway.catalog.model.StrategicGoalCard;
import com.portfolio.gateway.catalog.port.DomainProjectCatalog;
import com.portfolio.gateway.catalog.query.ProjectFilter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class StubDomainProjectCatalog implements DomainProjectCatalog {

    static final UUID PORTAL_ID = UUID.fromString("00000000-0000-4000-8000-000000000601");
    static final UUID LEDGER_ID = UUID.fromString("00000000-0000-4000-8000-000000000602");
    static final UUID OWNER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private static final DepartmentCard IT =
            new DepartmentCard(UUID.fromString("00000000-0000-4000-8000-000000000101"), "IT", "Информационные технологии");
    private static final DepartmentCard FIN =
            new DepartmentCard(UUID.fromString("00000000-0000-4000-8000-000000000102"), "FIN", "Финансы");
    private static final DepartmentCard OPS =
            new DepartmentCard(UUID.fromString("00000000-0000-4000-8000-000000000103"), "OPS", "Операции");
    private static final StrategicGoalCard ALIGN = new StrategicGoalCard(
            UUID.fromString("00000000-0000-4000-8000-000000000201"), "ALIGN", "Стратегическое соответствие");
    private static final StrategicGoalCard VALUE = new StrategicGoalCard(
            UUID.fromString("00000000-0000-4000-8000-000000000202"), "VALUE", "Экономический эффект");
    private static final StrategicGoalCard RISK =
            new StrategicGoalCard(UUID.fromString("00000000-0000-4000-8000-000000000203"), "RISK", "Снижение риска");

    private static final Map<String, DepartmentCard> DEPARTMENTS =
            Map.of("IT", IT, "FIN", FIN, "OPS", OPS);
    private static final Map<String, StrategicGoalCard> GOALS =
            Map.of("ALIGN", ALIGN, "VALUE", VALUE, "RISK", RISK);

    private static final Set<ProjectStatus> SUBMIT_FROM =
            EnumSet.of(ProjectStatus.DRAFT, ProjectStatus.RETURNED_FOR_REVISION);
    private static final Set<ProjectStatus> RETURN_OR_REJECT_FROM =
            EnumSet.of(ProjectStatus.SUBMITTED, ProjectStatus.UNDER_REVIEW, ProjectStatus.EVALUATED);
    private static final Set<ProjectStatus> ARCHIVE_FROM =
            EnumSet.of(ProjectStatus.DRAFT, ProjectStatus.REJECTED, ProjectStatus.RETURNED_FOR_REVISION);

    private final Map<UUID, ProjectSnapshot> projects = new ConcurrentHashMap<>();

    public StubDomainProjectCatalog() {
        reset();
    }

    public void reset() {
        projects.clear();
        projects.put(PORTAL_ID, portal());
        projects.put(LEDGER_ID, ledger());
    }

    @Override
    public Optional<ProjectSnapshot> findById(UUID id) {
        return Optional.ofNullable(projects.get(id));
    }

    @Override
    public List<ProjectSnapshot> findAll(ProjectFilter filter) {
        return projects.values().stream().filter(project -> matches(project, filter)).toList();
    }

    @Override
    public ProjectSnapshot register(RegisterInitiativeCommand command) {
        validateRegister(command);
        UUID ownerId;
        try {
            ownerId = UUID.fromString(command.ownerId());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidProjectIdException();
        }
        List<GoalContributionCard> goals = command.goals().stream()
                .map(item -> new GoalContributionCard(GOALS.get(item.goalCode()), item.level()))
                .toList();
        ProjectSnapshot snapshot = new ProjectSnapshot(
                UUID.randomUUID(),
                command.code(),
                command.title(),
                command.description(),
                ProjectStatus.DRAFT,
                money(parseBudget(command.requestedBudget())),
                DEPARTMENTS.get(command.departmentCode()),
                ownerId,
                goals);
        projects.put(snapshot.id(), snapshot);
        return snapshot;
    }

    @Override
    public ProjectSnapshot submit(UUID id) {
        return transition(id, SUBMIT_FROM, ProjectStatus.SUBMITTED);
    }

    @Override
    public ProjectSnapshot startReview(UUID id) {
        return transition(id, EnumSet.of(ProjectStatus.SUBMITTED), ProjectStatus.UNDER_REVIEW);
    }

    @Override
    public ProjectSnapshot completeEvaluation(UUID id) {
        return transition(id, EnumSet.of(ProjectStatus.UNDER_REVIEW), ProjectStatus.EVALUATED);
    }

    @Override
    public ProjectSnapshot approve(UUID id) {
        return transition(id, EnumSet.of(ProjectStatus.EVALUATED), ProjectStatus.APPROVED_FOR_PORTFOLIO);
    }

    @Override
    public ProjectSnapshot returnForRevision(UUID id, String reason) {
        return transition(id, RETURN_OR_REJECT_FROM, ProjectStatus.RETURNED_FOR_REVISION);
    }

    @Override
    public ProjectSnapshot reject(UUID id, String reason) {
        return transition(id, RETURN_OR_REJECT_FROM, ProjectStatus.REJECTED);
    }

    @Override
    public ProjectSnapshot archive(UUID id) {
        return transition(id, ARCHIVE_FROM, ProjectStatus.ARCHIVED);
    }

    @Override
    public ProjectRegistryStats stats(ProjectFilter filter) {
        List<ProjectSnapshot> items = findAll(filter);
        Map<ProjectStatus, Integer> counts = new EnumMap<>(ProjectStatus.class);
        Map<String, BigDecimal> budgetByDepartment = new LinkedHashMap<>();
        BigDecimal totalBudget = BigDecimal.ZERO.setScale(4, RoundingMode.UNNECESSARY);
        for (ProjectSnapshot item : items) {
            counts.merge(item.status(), 1, Integer::sum);
            BigDecimal budget = parseBudget(item.requestedBudget());
            totalBudget = totalBudget.add(budget);
            String departmentCode = item.department().code();
            budgetByDepartment.merge(departmentCode, budget, BigDecimal::add);
        }
        List<StatusCount> byStatus = new ArrayList<>();
        for (ProjectStatus status : ProjectStatus.values()) {
            Integer count = counts.get(status);
            if (count != null) {
                byStatus.add(new StatusCount(status, count));
            }
        }
        List<DepartmentBudgetStat> departmentStats = budgetByDepartment.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DepartmentBudgetStat(entry.getKey(), money(entry.getValue())))
                .toList();
        return new ProjectRegistryStats(items.size(), byStatus, money(totalBudget), departmentStats);
    }

    private ProjectSnapshot transition(UUID id, Set<ProjectStatus> allowed, ProjectStatus next) {
        ProjectSnapshot current = projects.get(id);
        if (current == null) {
            throw new ProjectNotFoundException(id);
        }
        if (!allowed.contains(current.status())) {
            throw new IllegalStatusTransitionException(current.status(), next);
        }
        ProjectSnapshot updated = new ProjectSnapshot(
                current.id(),
                current.code(),
                current.title(),
                current.description(),
                next,
                current.requestedBudget(),
                current.department(),
                current.ownerId(),
                current.goals());
        projects.put(id, updated);
        return updated;
    }

    private void validateRegister(RegisterInitiativeCommand command) {
        if (command.code() == null || command.code().isBlank()) {
            throw new CatalogValidationException("Project code is required");
        }
        if (command.title() == null || command.title().isBlank()) {
            throw new CatalogValidationException("Project title is required");
        }
        if (projects.values().stream().anyMatch(project -> project.code().equals(command.code()))) {
            throw new ConflictException("Project code already exists: " + command.code());
        }
        if (command.departmentCode() == null || !DEPARTMENTS.containsKey(command.departmentCode())) {
            throw new CatalogValidationException("Unknown department: " + command.departmentCode());
        }
        if (command.goals() == null || command.goals().isEmpty()) {
            throw new CatalogValidationException("At least one goal is required");
        }
        for (GoalContributionInput goal : command.goals()) {
            if (goal.goalCode() == null || !GOALS.containsKey(goal.goalCode())) {
                throw new CatalogValidationException("Unknown goal: " + goal.goalCode());
            }
            if (goal.level() < 1 || goal.level() > 5) {
                throw new CatalogValidationException("Goal level must be between 1 and 5");
            }
        }
        BigDecimal budget = parseBudget(command.requestedBudget());
        if (budget.compareTo(BigDecimal.ZERO) < 0) {
            throw new CatalogValidationException("Requested budget must be >= 0");
        }
    }

    private static boolean matches(ProjectSnapshot project, ProjectFilter filter) {
        boolean excludeArchived = filter == null || filter.excludeArchived() == null || filter.excludeArchived();
        if (excludeArchived && project.status() == ProjectStatus.ARCHIVED) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        boolean hasStatus = filter.status() != null;
        boolean hasStatuses = filter.statuses() != null && !filter.statuses().isEmpty();
        if (hasStatus || hasStatuses) {
            boolean statusMatch = hasStatus && project.status() == filter.status();
            boolean statusesMatch = hasStatuses && filter.statuses().contains(project.status());
            if (!statusMatch && !statusesMatch) {
                return false;
            }
        }
        if (filter.departmentCode() != null && !filter.departmentCode().isBlank()
                && !project.department().code().equals(filter.departmentCode())) {
            return false;
        }
        BigDecimal budget = parseBudget(project.requestedBudget());
        if (filter.minBudget() != null && !filter.minBudget().isBlank()
                && budget.compareTo(parseBudget(filter.minBudget())) < 0) {
            return false;
        }
        if (filter.maxBudget() != null && !filter.maxBudget().isBlank()
                && budget.compareTo(parseBudget(filter.maxBudget())) > 0) {
            return false;
        }
        if (filter.goalCode() != null && !filter.goalCode().isBlank()) {
            boolean hasGoal = project.goals().stream()
                    .anyMatch(item -> item.goal().code().equals(filter.goalCode()));
            if (!hasGoal) {
                return false;
            }
        }
        return true;
    }

    private static BigDecimal parseBudget(String raw) {
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException | NullPointerException ex) {
            throw new CatalogValidationException("Invalid budget: " + raw);
        }
    }

    private static String money(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private static ProjectSnapshot portal() {
        return new ProjectSnapshot(
                PORTAL_ID,
                "PRJ-PORTAL",
                "Портал самообслуживания",
                "Заявки сотрудников без участия оператора",
                ProjectStatus.SUBMITTED,
                "1200000.0000",
                IT,
                OWNER_ID,
                List.of(new GoalContributionCard(ALIGN, 5), new GoalContributionCard(VALUE, 3)));
    }

    private static ProjectSnapshot ledger() {
        return new ProjectSnapshot(
                LEDGER_ID,
                "PRJ-LEDGER",
                "Обновление учётной системы",
                "Замена контура финансового учёта",
                ProjectStatus.DRAFT,
                "900000.0000",
                FIN,
                OWNER_ID,
                List.of(new GoalContributionCard(RISK, 2), new GoalContributionCard(VALUE, 4)));
    }
}
