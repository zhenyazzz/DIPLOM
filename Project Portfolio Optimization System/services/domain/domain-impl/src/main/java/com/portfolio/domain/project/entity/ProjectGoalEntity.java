package com.portfolio.domain.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_goals", schema = "domain")
@Getter
@Setter
@NoArgsConstructor
public class ProjectGoalEntity {

    @EmbeddedId
    private ProjectGoalId id;

    @Column(name = "contribution_level", nullable = false)
    private short contributionLevel;
}
