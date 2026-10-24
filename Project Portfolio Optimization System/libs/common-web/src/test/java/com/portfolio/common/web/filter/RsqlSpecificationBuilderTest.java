package com.portfolio.common.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.common.web.support.ProjectEntity;
import com.portfolio.common.web.support.ProjectRepository;
import com.portfolio.common.web.support.ProjectStatus;
import com.portfolio.common.web.support.TestApplication;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class RsqlSpecificationBuilderTest {

    @Autowired
    ProjectRepository repository;

    private final RsqlSpecificationBuilder builder = new RsqlSpecificationBuilder();
    private final FieldAllowList allowList = FieldAllowList.of(
            FieldRule.filterAndSort("name", String.class),
            FieldRule.filterAndSort("status", ProjectStatus.class),
            FieldRule.filterAndSort("budget", BigDecimal.class)
    );

    @BeforeEach
    void seed() {
        repository.save(project("Alpha", ProjectStatus.ACTIVE, "1000"));
        repository.save(project("Beta", ProjectStatus.DRAFT, "2500"));
        repository.save(project("Gamma", ProjectStatus.ACTIVE, "5000"));
    }

    @Test
    void andOrLikeEnumsAndNumbers() {
        Specification<ProjectEntity> spec = builder.build(
                "status==ACTIVE;(budget=ge=1000,name=ilike=%beta%)", allowList);
        List<ProjectEntity> found = repository.findAll(spec);
        assertThat(found).extracting(ProjectEntity::getName).containsExactlyInAnyOrder("Alpha", "Gamma");

        Specification<ProjectEntity> like = builder.build("name=like=A%", allowList);
        assertThat(repository.findAll(like)).extracting(ProjectEntity::getName).containsExactly("Alpha");
    }

    @Test
    void unknownFieldRejected() {
        assertThatThrownBy(() -> repository.findAll(builder.build("createdBy==x", allowList)))
                .isInstanceOf(FilterBadRequestException.class);
    }

    @Test
    void nestedPathRejected() {
        assertThatThrownBy(() -> repository.findAll(builder.build("author.name==x", allowList)))
                .isInstanceOf(FilterBadRequestException.class);
    }

    private static ProjectEntity project(String name, ProjectStatus status, String budget) {
        ProjectEntity entity = new ProjectEntity();
        entity.setName(name);
        entity.setStatus(status);
        entity.setBudget(new BigDecimal(budget));
        return entity;
    }
}
