package org.example.api.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.api.controllers.helpers.ControllerHelper;
import org.example.api.dto.TaskStateDto;
import org.example.api.exceptions.BadRequestException;
import org.example.api.mappers.TaskStateDtoMapper;
import org.example.store.entities.ProjectEntity;
import org.example.store.entities.TaskStateEntity;
import org.example.store.entities.UserEntity;
import org.example.store.repositories.TaskStateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Transactional
@RestController
public class TaskStateController {

    private final TaskStateRepository taskStateRepository;
    private final TaskStateDtoMapper taskStateDtoMapper;
    private final ControllerHelper controllerHelper;

    public static final String GET_TASK_STATES = "/api/projects/{project_id}/task_states";
    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/task_states";
    public static final String UPDATE_TASK_STATE = "/api/projects/{project_id}/task_states/{task_state_id}";
    public static final String CHANGE_TASK_STATE_POSITION = "/api/projects/{project_id}/task_states/{task_state_id}/position";
    private static final String DELETE_TASK_STATE = "/api/projects/{project_id}/task-states/{task_state_id}";

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable("project_id") Long projectId) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        final ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        if (!Objects.equals(project.getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project's info.");
        }

        return project
                .getTaskStates()
                .stream()
                .map(taskStateDtoMapper::createTaskStateDto)
                .toList();
    }

    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskSate(
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam(name = "task_state_name") String taskStateName
    ) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        final ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task state name can't be empty.");
        }

        if (!Objects.equals(project.getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project.");
        }

        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();

        for (TaskStateEntity taskState : project.getTaskStates()) {

            if (taskState.getName().equalsIgnoreCase(taskStateName)) {
                throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
            }

            if (taskState.getRightTaskState().isEmpty()) {
                optionalAnotherTaskState = Optional.of(taskState);
                break;
            }
        }

        TaskStateEntity taskState = taskStateRepository.saveAndFlush(
                TaskStateEntity.builder()
                        .name(taskStateName)
                        .project(project)
                        .build()
        );

        optionalAnotherTaskState
                .ifPresent(anotherTaskState -> {

                    taskState.setLeftTaskState(anotherTaskState);

                    anotherTaskState.setRightTaskState(taskState);

                    taskStateRepository.saveAndFlush(anotherTaskState);
                });

        final TaskStateEntity savedTaskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoMapper.createTaskStateDto(savedTaskState);
    }

    @PatchMapping(UPDATE_TASK_STATE)
    public TaskStateDto updateTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName
    ) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        final ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        if (!Objects.equals(project.getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project.");
        }

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task state name can't be empty.");
        }

        TaskStateEntity taskState = controllerHelper.getTaskStateIdOrThrowException(taskStateId);

        taskStateRepository
                .findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(
                        taskState.getProject().getId(),
                        taskStateName
                )
                .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId))
                .ifPresent(anotherTaskState -> {
                    throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
                });

        taskState.setName(taskStateName);

        taskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoMapper.createTaskStateDto(taskState);
    }

    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(
            @PathVariable(name = "project_id") Long projectId,
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId
    ) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        final ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        if (!Objects.equals(project.getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project.");
        }

        TaskStateEntity taskToChange = controllerHelper
                .getTaskStateIdOrThrowException(taskStateId);

        Optional<Long> optionalOldLeftTaskStateId = taskToChange
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        if (optionalOldLeftTaskStateId.equals(optionalLeftTaskStateId)) {
            return taskStateDtoMapper.createTaskStateDto(taskToChange);
        }

        Optional<TaskStateEntity> optionalNewLeftTaskState = optionalLeftTaskStateId
                .map(leftTaskStateId -> {

                    if (taskStateId.equals(leftTaskStateId)) {
                        throw new BadRequestException("Left task state id equals changed task state.");
                    }

                    TaskStateEntity leftTaskStateEntity = controllerHelper
                            .getTaskStateIdOrThrowException(leftTaskStateId);

                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) {
                        throw new BadRequestException("Task state position can be changed within the same project.");
                    }

                    return leftTaskStateEntity;
                });

        Optional<TaskStateEntity> optionalNewRightTaskState;
        if (optionalNewLeftTaskState.isEmpty()) {

            optionalNewRightTaskState = project
                    .getTaskStates()
                    .stream()
                    .filter(anotherTaskState -> anotherTaskState.getLeftTaskState().isEmpty())
                    .findAny();
        } else {

            optionalNewRightTaskState = optionalNewLeftTaskState
                    .get()
                    .getRightTaskState();
        }

        replaceOldTaskStatePosition(taskToChange);

        if (optionalNewLeftTaskState.isPresent()) {

            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();

            newLeftTaskState.setRightTaskState(taskToChange);

            taskToChange.setLeftTaskState(newLeftTaskState);
        } else {
            taskToChange.setLeftTaskState(null);
        }

        if (optionalNewRightTaskState.isPresent()) {

            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();

            newRightTaskState.setLeftTaskState(taskToChange);

            taskToChange.setRightTaskState(newRightTaskState);
        } else {
            taskToChange.setRightTaskState(null);
        }

        taskToChange = taskStateRepository.saveAndFlush(taskToChange);

        optionalNewLeftTaskState
                .ifPresent(taskStateRepository::saveAndFlush);

        optionalNewRightTaskState
                .ifPresent(taskStateRepository::saveAndFlush);

        return taskStateDtoMapper.createTaskStateDto(taskToChange);
    }

    @DeleteMapping(DELETE_TASK_STATE)
    public ResponseEntity<String> deleteTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @PathVariable(name = "task_state_id"
            ) Long taskStateId) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        final ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        if (!Objects.equals(project.getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project.");
        }

        TaskStateEntity changeTaskState = controllerHelper.getTaskStateIdOrThrowException(taskStateId);

        replaceOldTaskStatePosition(changeTaskState);

        taskStateRepository.delete(changeTaskState);

        return ResponseEntity.ok(String.format("Task state with id - \"%s\" was successfully deleted", taskStateId));
    }

    private void replaceOldTaskStatePosition(TaskStateEntity changeTaskState) {

        Optional<TaskStateEntity> optionalOldLeftTaskState = changeTaskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalOldRightTaskState = changeTaskState.getRightTaskState();

        optionalOldLeftTaskState
                .ifPresent(it -> {

                    it.setRightTaskState(optionalOldRightTaskState.orElse(null));

                    taskStateRepository.saveAndFlush(it);
                });

        optionalOldRightTaskState
                .ifPresent(it -> {

                    it.setLeftTaskState(optionalOldLeftTaskState.orElse(null));

                    taskStateRepository.saveAndFlush(it);
                });
    }

}
