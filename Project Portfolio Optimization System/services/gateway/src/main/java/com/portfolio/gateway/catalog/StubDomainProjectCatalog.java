package com.portfolio.gateway.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private static final StrategicGoalCard ALIGN = new StrategicGoalCard(
            UUID.fromString("00000000-0000-4000-8000-000000000201"), "ALIGN", "Стратегическое соответствие");
    private static final StrategicGoalCard VALUE = new StrategicGoalCard(
            UUID.fromString("00000000-0000-4000-8000-000000000202"), "VALUE", "Экономический эффект");
    private static final StrategicGoalCard RISK =
            new StrategicGoalCard(UUID.fromString("00000000-0000-4000-8000-000000000203"), "RISK", "Снижение риска");

    private static final List<ProjectSnapshot> PROJECTS = List.of(
            new ProjectSnapshot(
                    PORTAL_ID,
                    "PRJ-PORTAL",
                    "Портал самообслуживания",
                    "Заявки сотрудников без участия оператора",
                    ProjectStatus.SUBMITTED,
                    "1200000.0000",
                    IT,
                    OWNER_ID,
                    List.of(new GoalContributionCard(ALIGN, 5), new GoalContributionCard(VALUE, 3))),
            new ProjectSnapshot(
                    LEDGER_ID,
                    "PRJ-LEDGER",
                    "Обновление учётной системы",
                    "Замена контура финансового учёта",
                    ProjectStatus.DRAFT,
                    "900000.0000",
                    FIN,
                    OWNER_ID,
                    List.of(new GoalContributionCard(RISK, 2), new GoalContributionCard(VALUE, 4))));

    @Override
    public Optional<ProjectSnapshot> findById(UUID id) {
        return PROJECTS.stream().filter(project -> project.id().equals(id)).findFirst();
    }

    @Override
    public List<ProjectSnapshot> findAll(ProjectStatus status) {
        if (status == null) {
            return PROJECTS;
        }
        return PROJECTS.stream().filter(project -> project.status() == status).toList();
    }
}
