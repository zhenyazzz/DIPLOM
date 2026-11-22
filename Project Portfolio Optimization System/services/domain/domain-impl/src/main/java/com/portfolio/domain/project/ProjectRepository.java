package com.portfolio.domain.project;

import com.portfolio.domain.project.entity.ProjectEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    @Query("select p from ProjectEntity p join fetch p.department order by p.code")
    List<ProjectEntity> findAllWithDepartment();

    @Query("select p from ProjectEntity p join fetch p.department where p.status = :status order by p.code")
    List<ProjectEntity> findByStatusWithDepartment(@Param("status") String status);

    @Query("select p from ProjectEntity p join fetch p.department where p.id = :id")
    Optional<ProjectEntity> findWithDepartmentById(@Param("id") UUID id);

    boolean existsByCode(String code);

    Optional<ProjectEntity> findByCode(String code);
}
