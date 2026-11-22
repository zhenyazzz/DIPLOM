package com.portfolio.domain.project.entity;

import com.portfolio.common.web.domain.BaseEntity;
import com.portfolio.domain.reference.entity.DepartmentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects", schema = "domain")
@Getter
@Setter
@NoArgsConstructor
public class ProjectEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 250)
    private String title;

    @Column
    private String description;

    @Column(name = "business_case")
    private String businessCase;

    @Column(nullable = false, length = 40)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "planned_start")
    private LocalDate plannedStart;

    @Column(name = "planned_finish")
    private LocalDate plannedFinish;

    @Column(name = "requested_budget", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedBudget;

    @Column(name = "expected_benefit", precision = 19, scale = 4)
    private BigDecimal expectedBenefit;

    @Column(name = "risk_category", length = 20)
    private String riskCategory;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;
}
