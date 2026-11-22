package com.portfolio.domain.reference.entity;

import com.portfolio.common.web.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "strategic_goals", schema = "domain")
@Getter
@Setter
@NoArgsConstructor
public class StrategicGoalEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private int priority = 1;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
