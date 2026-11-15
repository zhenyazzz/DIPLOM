package com.portfolio.identity.identity.repository;

import com.portfolio.identity.identity.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query(
            """
            select count(u) from UserEntity u
            where u.role.code = :roleCode and u.blocked = false
            """)
    long countActiveUsersWithRole(@Param("roleCode") String roleCode);
}
