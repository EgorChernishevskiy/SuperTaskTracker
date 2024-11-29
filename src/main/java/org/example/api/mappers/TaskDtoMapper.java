package org.example.api.mappers;

import org.example.api.dto.TaskDto;
import org.example.store.entities.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskDtoMapper {

    public TaskDto createTaskDto(TaskEntity taskEntity) {

        return TaskDto.builder()
                .id(taskEntity.getId())
                .name(taskEntity.getName())
                .description(taskEntity.getDescription())
                .position(taskEntity.getPosition())
                .build();
    }
}