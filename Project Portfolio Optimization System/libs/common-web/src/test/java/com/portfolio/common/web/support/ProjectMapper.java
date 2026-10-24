package com.portfolio.common.web.support;

import com.portfolio.common.web.crud.EntityMapper;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper
        implements EntityMapper<ProjectEntity, CreateProjectRequest, UpdateProjectRequest, ProjectResponse> {

    @Override
    public ProjectEntity toEntity(CreateProjectRequest createRequest) {
        ProjectEntity entity = new ProjectEntity();
        entity.setName(createRequest.name());
        entity.setStatus(createRequest.status());
        entity.setBudget(createRequest.budget());
        return entity;
    }

    @Override
    public void updateEntity(UpdateProjectRequest updateRequest, ProjectEntity entity) {
        if (updateRequest.name() != null) {
            entity.setName(updateRequest.name());
        }
        if (updateRequest.status() != null) {
            entity.setStatus(updateRequest.status());
        }
        if (updateRequest.budget() != null) {
            entity.setBudget(updateRequest.budget());
        }
    }

    @Override
    public ProjectResponse toResponse(ProjectEntity entity) {
        return new ProjectResponse(
                entity.getId(),
                entity.getName(),
                entity.getStatus(),
                entity.getBudget(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
    }
}
