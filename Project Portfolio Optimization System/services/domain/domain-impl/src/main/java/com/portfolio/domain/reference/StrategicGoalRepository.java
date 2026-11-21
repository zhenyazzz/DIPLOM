package com.portfolio.domain.reference;

import com.portfolio.domain.reference.entity.StrategicGoalEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategicGoalRepository extends JpaRepository<StrategicGoalEntity, UUID> {}
