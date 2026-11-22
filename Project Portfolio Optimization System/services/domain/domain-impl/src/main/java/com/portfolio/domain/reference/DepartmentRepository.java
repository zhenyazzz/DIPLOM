package com.portfolio.domain.reference;

import com.portfolio.domain.reference.entity.DepartmentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    Optional<DepartmentEntity> findByCode(String code);

    boolean existsByCode(String code);
}
