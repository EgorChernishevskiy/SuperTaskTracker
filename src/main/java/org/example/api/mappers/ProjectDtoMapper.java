package org.example.api.mappers;

import org.example.api.dto.ProjectDto;
import org.example.store.entities.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectDtoMapper {

    public final ProjectDto createProjectDto(ProjectEntity entity) {

        return ProjectDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
