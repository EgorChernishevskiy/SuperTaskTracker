package org.example.api.mappers;

import lombok.RequiredArgsConstructor;
import org.example.api.dto.TaskStateDto;
import org.example.store.entities.TaskStateEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class TaskStateDtoMapper {

    private final TaskDtoMapper taskDtoMapper;

    public TaskStateDto createTaskStateDto(TaskStateEntity entity) {

        return TaskStateDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .leftTaskStateId(entity.getLeftTaskState().map(TaskStateEntity::getId).orElse(null))
                .rightTaskStateId(entity.getRightTaskState().map(TaskStateEntity::getId).orElse(null))
                .tasks(
                        entity
                                .getTasks()
                                .stream()
                                .map(taskDtoMapper::createTaskDto)
                                .collect(Collectors.toList())
                )
                .build();
    }
}
