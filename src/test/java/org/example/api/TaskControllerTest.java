package org.example.api;

import org.example.api.controllers.TaskController;
import org.example.api.controllers.helpers.ControllerHelper;
import org.example.api.dto.TaskDto;
import org.example.api.exceptions.BadRequestException;
import org.example.api.mappers.TaskDtoMapper;
import org.example.store.entities.ProjectEntity;
import org.example.store.entities.TaskEntity;
import org.example.store.entities.TaskStateEntity;
import org.example.store.entities.UserEntity;
import org.example.store.repositories.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TaskControllerTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskDtoMapper taskDtoMapper;

    @Mock
    private ControllerHelper controllerHelper;

    @InjectMocks
    private TaskController taskController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getTasks_successful() {

        Long taskStateId = 1L;

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        TaskStateEntity taskState = new TaskStateEntity();
        taskState.setId(taskStateId);
        taskState.setProject(
                new ProjectEntity(
                        1L,
                        "TestProject",
                        Instant.now(),
                        Instant.now(),
                        List.of(),
                        currentUser
                ));
        taskState.setTasks(List.of(
                new TaskEntity(1L, "Task1", "Desc1", 0, taskState),
                new TaskEntity(2L, "Task2", "Desc2", 1, taskState)
        ));

        when(controllerHelper.getCurrentUser()).
                thenReturn(currentUser);

        when(controllerHelper.getTaskStateIdOrThrowException(taskStateId)).
                thenReturn(taskState);

        when(taskDtoMapper.createTaskDto(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity task = invocation.getArgument(0);
            return new TaskDto(task.getId(), task.getName(), task.getPosition(), task.getDescription());
        });

        List<TaskDto> result = taskController.getTasks(taskStateId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Task1", result.get(0).getName());
        assertEquals("Task2", result.get(1).getName());

        verify(controllerHelper)
                .getCurrentUser();

        verify(controllerHelper)
                .getTaskStateIdOrThrowException(taskStateId);
    }

    @Test
    void getTasks_unauthorizedUser_throwsException() {

        Long taskStateId = 1L;

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        UserEntity anotherUser = new UserEntity();
        anotherUser.setId(2L);

        TaskStateEntity taskState = new TaskStateEntity();
        taskState.setId(taskStateId);
        taskState.setProject(
                new ProjectEntity(
                        1L,
                        "TestProject",
                        Instant.now(),
                        Instant.now(),
                        List.of(),
                        anotherUser
                ));

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(controllerHelper.getTaskStateIdOrThrowException(taskStateId))
                .thenReturn(taskState);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> taskController.getTasks(taskStateId));

        assertEquals("You do not have permission to access this project.", exception.getMessage());

        verify(controllerHelper)
                .getCurrentUser();

        verify(controllerHelper)
                .getTaskStateIdOrThrowException(taskStateId);
    }

    @Test
    void createTask_successful() {

        Long taskStateId = 1L;

        String taskName = "New Task";
        String description = "Task description";

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        TaskStateEntity taskState = new TaskStateEntity();
        taskState.setId(taskStateId);
        taskState.setTasks(new ArrayList<>());
        taskState.setProject(
                new ProjectEntity(
                        1L,
                        "TestProject",
                        Instant.now(),
                        Instant.now(),
                        List.of(),
                        currentUser
                ));

        TaskEntity savedTask = new TaskEntity(1L, taskName, description, 0, taskState);

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(controllerHelper.getTaskStateIdOrThrowException(taskStateId))
                .thenReturn(taskState);

        when(taskRepository.saveAndFlush(any(TaskEntity.class)))
                .thenReturn(savedTask);

        when(taskDtoMapper.createTaskDto(savedTask))
                .thenReturn(new TaskDto(1L, taskName, 0, description));

        TaskDto result = taskController.createTask(taskStateId, taskName, description);

        assertNotNull(result);
        assertEquals(taskName, result.getName());
        assertEquals(description, result.getDescription());

        verify(taskRepository)
                .saveAndFlush(any(TaskEntity.class));

        verify(taskDtoMapper)
                .createTaskDto(savedTask);
    }

    @Test
    void createTask_emptyName_throwsException() {

        Long taskStateId = 1L;

        String description = "Task description";

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> taskController.createTask(taskStateId, "", description));

        assertEquals("Task's name can't be empty.", exception.getMessage());

        verifyZeroInteractions(taskRepository);
    }

    @Test
    void createTask_emptyDescription_throwsException() {

        Long taskStateId = 1L;

        String name = "Task name";

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> taskController.createTask(taskStateId, name, ""));

        assertEquals("Task's description can't be empty.", exception.getMessage());

        verifyZeroInteractions(taskRepository);
    }

    @Test
    void updateTask_successful() {

        Long taskId = 1L;
        String newName = "Updated Task";
        String newDescription = "Updated Description";

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        TaskStateEntity taskState = new TaskStateEntity();
        taskState.setProject(
                new ProjectEntity(
                        1L,
                        "TestProject",
                        Instant.now(),
                        Instant.now(),
                        List.of(),
                        currentUser
                ));

        TaskEntity taskToUpdate = new TaskEntity(
                taskId,
                "Old Task",
                "Old Description",
                0,
                taskState
        );

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(controllerHelper.getTaskIdOrThrowException(taskId))
                .thenReturn(taskToUpdate);

        when(taskRepository.saveAndFlush(any(TaskEntity.class)))
                .thenReturn(taskToUpdate);

        when(taskDtoMapper.createTaskDto(taskToUpdate))
                .thenReturn(new TaskDto(taskId, newName, 0, newDescription));

        TaskDto result = taskController.updateTask(taskId, newName, newDescription);

        assertNotNull(result);
        assertEquals(newName, taskToUpdate.getName());
        assertEquals(newDescription, taskToUpdate.getDescription());

        verify(taskRepository)
                .saveAndFlush(taskToUpdate);
    }

    @Test
    void deleteTask_successful() {
        Long taskId = 1L;

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        TaskStateEntity taskState = new TaskStateEntity();
        taskState.setTasks(new ArrayList<>());
        taskState.setProject(new ProjectEntity(
                1L,
                "TestProject",
                Instant.now(),
                Instant.now(),
                List.of(),
                currentUser
        ));

        TaskEntity taskToDelete = new TaskEntity(
                taskId,
                "Task to delete",
                "Description",
                0,
                taskState);

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(controllerHelper.getTaskIdOrThrowException(taskId))
                .thenReturn(taskToDelete);

        ResponseEntity<String> response = taskController.deleteTask(taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Task with id - \"1\" was successfully deleted", response.getBody());

        verify(taskRepository).delete(taskToDelete);
    }

    @Test
    void changeTaskPosition_successful() {

        Long taskId = 1L;
        int newPosition = 1;

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        ProjectEntity project = new ProjectEntity();
        project.setAppUser(currentUser);

        TaskStateEntity taskState = new TaskStateEntity();
        taskState.setProject(project);
        taskState.setTasks(new ArrayList<>());

        TaskEntity task1 = new TaskEntity(1L, "Task1", "Description1", 0, taskState);
        TaskEntity task2 = new TaskEntity(2L, "Task2", "Description2", 1, taskState);
        TaskEntity task3 = new TaskEntity(3L, "Task3", "Description3", 2, taskState);

        taskState.setTasks(new ArrayList<>(List.of(task1, task2, task3)));

        when(controllerHelper.getCurrentUser()).thenReturn(currentUser);
        when(controllerHelper.getTaskIdOrThrowException(taskId)).thenReturn(task1);

        when(taskRepository.saveAndFlush(any(TaskEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(taskDtoMapper.createTaskDto(task1))
                .thenReturn(new TaskDto(1L, "Task1", newPosition, "Description1"));

        TaskDto result = taskController.changeTaskPosition(taskId, newPosition);

        assertNotNull(result);
        assertEquals(newPosition, task1.getPosition());
        assertEquals(0, taskState.getTasks().get(0).getPosition());
        assertEquals(1, taskState.getTasks().get(1).getPosition());
        assertEquals(2, taskState.getTasks().get(2).getPosition());

        verify(taskRepository, times(3))
                .saveAndFlush(any(TaskEntity.class));
    }
}
