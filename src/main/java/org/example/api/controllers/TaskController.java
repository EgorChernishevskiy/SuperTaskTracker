package org.example.api.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.api.controllers.helpers.ControllerHelper;
import org.example.api.dto.TaskDto;
import org.example.api.exceptions.BadRequestException;
import org.example.api.mappers.TaskDtoMapper;
import org.example.store.entities.TaskEntity;
import org.example.store.entities.TaskStateEntity;
import org.example.store.entities.UserEntity;
import org.example.store.repositories.TaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;
import java.util.Objects;

@RequiredArgsConstructor
@Transactional
@RestController
public class TaskController {

    private final TaskRepository taskRepository;
    private final TaskDtoMapper taskDtoMapper;
    private final ControllerHelper controllerHelper;

    public static final String GET_TASKS = "/api/task_states/{task_state_id}/tasks";
    public static final String CREATE_TASK = "/api/task_states/{task_state_id}/tasks";
    public static final String UPDATE_TASK = "/api/tasks/{task_id}";
    public static final String CHANGE_TASK_POSITION = "/api/tasks/{task_id}/position";
    private static final String DELETE_TASK = "/api/tasks/{task_id}";

    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable("task_state_id") Long taskStateId) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        TaskStateEntity taskState = controllerHelper.getTaskStateIdOrThrowException(taskStateId);

        if (!Objects.equals(taskState.getProject().getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project.");
        }


        return taskState
                .getTasks()
                .stream()
                .sorted(Comparator.comparingInt(TaskEntity::getPosition))
                .map(taskDtoMapper::createTaskDto)
                .toList();
    }

    @PostMapping(CREATE_TASK)
    public TaskDto createTask(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_name") String taskName,
            @RequestParam(name = "description") String description) {

        if (taskName.isBlank()) {
            throw new BadRequestException("Task's name can't be empty.");
        }

        if (description.isBlank()) {
            throw new BadRequestException("Task's description can't be empty.");
        }

        TaskStateEntity taskState = controllerHelper.getTaskStateIdOrThrowException(taskStateId);

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        if (!Objects.equals(taskState.getProject().getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project.");
        }

        int newPosition = taskState.getTasks().size();

        TaskEntity newTask = TaskEntity.builder()
                .name(taskName)
                .description(description)
                .position(newPosition)
                .taskState(taskState)
                .build();

        TaskEntity savedTask = taskRepository.saveAndFlush(newTask);
        return taskDtoMapper.createTaskDto(savedTask);
    }

    @PatchMapping(UPDATE_TASK)
    public TaskDto updateTask(
            @PathVariable(name = "task_id") Long taskId,
            @RequestParam(name = "task_name", required = false) String taskName,
            @RequestParam(name = "description", required = false) String description) {

        TaskEntity taskToUpdate = controllerHelper.getTaskIdOrThrowException(taskId);

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        if (!Objects.equals(taskToUpdate.getTaskState().getProject().getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project.");
        }

        if (taskName != null && !taskName.isBlank()) {
            taskToUpdate.setName(taskName);
        } else if (taskName != null) {
            throw new BadRequestException("Task's name can't be empty.");
        }

        if (description != null && !description.isBlank()) {
            taskToUpdate.setDescription(description);
        } else if (description != null) {
            throw new BadRequestException("Task's description can't be empty.");
        }

        TaskEntity updatedTask = taskRepository.saveAndFlush(taskToUpdate);
        return taskDtoMapper.createTaskDto(updatedTask);
    }

    @PatchMapping(CHANGE_TASK_POSITION)
    public TaskDto changeTaskPosition(
            @PathVariable(name = "task_id") Long taskId,
            @RequestParam(name = "new_position") int newPosition) {

        TaskEntity taskToChange = controllerHelper.getTaskIdOrThrowException(taskId);
        TaskStateEntity taskState = taskToChange.getTaskState();

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        if (!Objects.equals(taskState.getProject().getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project.");
        }

        List<TaskEntity> tasks = taskState.getTasks();

        if (newPosition < 0 || newPosition >= tasks.size()) {
            throw new BadRequestException("Invalid position value.");
        }

        tasks.remove(taskToChange);
        tasks.add(newPosition, taskToChange);

        for (int i = 0; i < tasks.size(); i++) {
            TaskEntity task = tasks.get(i);
            task.setPosition(i);
            taskRepository.saveAndFlush(task);
        }

        return taskDtoMapper.createTaskDto(taskToChange);
    }

    @DeleteMapping(DELETE_TASK)
    public ResponseEntity<String> deleteTask(@PathVariable(name = "task_id") Long taskId) {

        TaskEntity taskToDelete = controllerHelper.getTaskIdOrThrowException(taskId);
        TaskStateEntity taskState = taskToDelete.getTaskState();

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        if (!Objects.equals(taskState.getProject().getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to access this project.");
        }

        List<TaskEntity> tasks = taskState.getTasks();

        tasks.remove(taskToDelete);
        taskRepository.delete(taskToDelete);

        for (int i = 0; i < tasks.size(); i++) {
            TaskEntity task = tasks.get(i);
            task.setPosition(i);
            taskRepository.saveAndFlush(task);
        }

        return ResponseEntity.ok(String.format("Task with id - \"%s\" was successfully deleted", taskId));
    }
}