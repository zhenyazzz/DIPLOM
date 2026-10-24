package com.portfolio.common.web.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.common.web.support.ProjectEntity;
import com.portfolio.common.web.support.ProjectRepository;
import com.portfolio.common.web.support.ProjectStatus;
import com.portfolio.common.web.support.TestApplication;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class BaseEntityAuditTest {

    @Autowired
    ProjectRepository repository;

    @Test
    void fillsAuditFieldsOnInsertAndUpdate() {
        ProjectEntity entity = new ProjectEntity();
        entity.setName("Alpha");
        entity.setStatus(ProjectStatus.DRAFT);
        entity.setBudget(new BigDecimal("1000.00"));

        ProjectEntity saved = repository.saveAndFlush(entity);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");
        assertThat(saved.getUpdatedBy()).isEqualTo("test-user");
        assertThat(saved.getVersion()).isNotNull();

        Instant createdAt = saved.getCreatedAt();
        Instant updatedAt = saved.getUpdatedAt();

        saved.setName("Alpha-2");
        ProjectEntity updated = repository.saveAndFlush(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
        assertThat(updated.getName()).isEqualTo("Alpha-2");
    }
}
