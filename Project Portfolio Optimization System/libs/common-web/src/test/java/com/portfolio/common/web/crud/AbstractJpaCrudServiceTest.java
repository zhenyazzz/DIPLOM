package com.portfolio.common.web.crud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.common.web.support.CreateProjectRequest;
import com.portfolio.common.web.support.ProjectResponse;
import com.portfolio.common.web.support.ProjectService;
import com.portfolio.common.web.support.ProjectStatus;
import com.portfolio.common.web.support.TestApplication;
import com.portfolio.common.web.support.UpdateProjectRequest;
import com.portfolio.common.web.web.NotFoundException;
import com.portfolio.common.web.web.PageResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class AbstractJpaCrudServiceTest {

    @Autowired
    ProjectService service;

    @Test
    void crudHappyPath() {
        ProjectResponse created = service.create(
                new CreateProjectRequest("Bridge", ProjectStatus.ACTIVE, new BigDecimal("5000.00")));
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Bridge");

        ProjectResponse loaded = service.get(created.id());
        assertThat(loaded.budget()).isEqualByComparingTo("5000.00");

        ProjectResponse updated = service.update(
                created.id(), new UpdateProjectRequest("Bridge-2", null, null));
        assertThat(updated.name()).isEqualTo("Bridge-2");
        assertThat(updated.status()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(updated.budget()).isEqualByComparingTo("5000.00");

        service.delete(created.id());
        assertThatThrownBy(() -> service.get(created.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getMissingThrowsNotFound() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchPageSortFilter() {
        service.create(new CreateProjectRequest("A", ProjectStatus.ACTIVE, new BigDecimal("1000")));
        service.create(new CreateProjectRequest("B", ProjectStatus.ACTIVE, new BigDecimal("3000")));
        service.create(new CreateProjectRequest("C", ProjectStatus.DRAFT, new BigDecimal("2000")));
        service.create(new CreateProjectRequest("D", ProjectStatus.ACTIVE, new BigDecimal("4000")));
        service.create(new CreateProjectRequest("E", ProjectStatus.ARCHIVED, new BigDecimal("5000")));

        PageResponse<ProjectResponse> page = service.search(
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "budget")),
                "status==ACTIVE;budget=ge=1000");

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).budget()).isEqualByComparingTo("4000");
        assertThat(page.content().get(1).budget()).isEqualByComparingTo("3000");
        assertThat(page.totalPages()).isEqualTo(2);
    }
}
