package com.portfolio.domain.project;

import com.portfolio.domain.project.entity.ProjectGoalEntity;
import com.portfolio.domain.project.entity.ProjectGoalId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectGoalRepository extends JpaRepository<ProjectGoalEntity, ProjectGoalId> {

    @Query("select link from ProjectGoalEntity link where link.id.projectId in :projectIds")
    List<ProjectGoalEntity> findByProjectIds(@Param("projectIds") Collection<UUID> projectIds);
}
