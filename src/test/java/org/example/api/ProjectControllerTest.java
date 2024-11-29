package org.example.api;

import org.example.api.controllers.ProjectController;
import org.example.api.controllers.helpers.ControllerHelper;
import org.example.api.dto.ProjectDto;
import org.example.api.exceptions.BadRequestException;
import org.example.api.mappers.ProjectDtoMapper;
import org.example.store.entities.ProjectEntity;
import org.example.store.entities.UserEntity;
import org.example.store.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProjectControllerTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectDtoMapper projectDtoMapper;

    @Mock
    private ControllerHelper controllerHelper;

    @InjectMocks
    private ProjectController projectController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void fetchProjects_no_prefix_successful() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        List<ProjectEntity> projects = List.of(
                new ProjectEntity(1L, "p1", Instant.now(), Instant.now(), List.of(), currentUser),
                new ProjectEntity(2L, "p2", Instant.now(), Instant.now(), List.of(), currentUser)
        );

        List<ProjectDto> projectsDto = List.of(
                new ProjectDto(1L, "p1", Instant.now(), Instant.now()),
                new ProjectDto(2L, "p2", Instant.now(), Instant.now())
        );

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(projectRepository.streamAllByAppUser(currentUser))
                .thenReturn(projects.stream());

        when(projectDtoMapper.createProjectDto(projects.get(0)))
                .thenReturn(projectsDto.get(0));

        when(projectDtoMapper.createProjectDto(projects.get(1)))
                .thenReturn(projectsDto.get(1));

        List<ProjectDto> result = projectController.fetchProjects(Optional.empty());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("p1", result.get(0).getName());
        assertEquals("p2", result.get(1).getName());

        verify(projectRepository)
                .streamAllByAppUser(currentUser);

        verify(projectDtoMapper, times(2))
                .createProjectDto(any(ProjectEntity.class));
    }

    @Test
    void fetchProjects_with_prefix_successful() {

        String prefix = "p1";

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        List<ProjectEntity> projects = List.of(
                new ProjectEntity(1L, "p1", Instant.now(), Instant.now(), List.of(), currentUser),
                new ProjectEntity(2L, "p2", Instant.now(), Instant.now(), List.of(), currentUser)
        );

        List<ProjectDto> projectsDto = List.of(
                new ProjectDto(1L, "p1", Instant.now(), Instant.now()),
                new ProjectDto(2L, "p2", Instant.now(), Instant.now())
        );

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(projectRepository.streamAllByNameStartsWithIgnoreCaseAndAppUser(prefix, currentUser))
                .thenReturn(Stream.of(projects.getFirst()));

        when(projectDtoMapper.createProjectDto(projects.getFirst()))
                .thenReturn(projectsDto.getFirst());

        List<ProjectDto> result = projectController.fetchProjects(Optional.of(prefix));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("p1", result.getFirst().getName());

        verify(projectRepository)
                .streamAllByNameStartsWithIgnoreCaseAndAppUser(prefix, currentUser);

        verify(projectDtoMapper, times(1))
                .createProjectDto(any(ProjectEntity.class));
    }

    @Test
    void fetchProjects_empty_prefix_successful() {

        String prefix = "";

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        List<ProjectEntity> projects = List.of(
                new ProjectEntity(1L, "p1", Instant.now(), Instant.now(), List.of(), currentUser),
                new ProjectEntity(2L, "p2", Instant.now(), Instant.now(), List.of(), currentUser)
        );

        List<ProjectDto> projectsDto = List.of(
                new ProjectDto(1L, "p1", Instant.now(), Instant.now()),
                new ProjectDto(2L, "p2", Instant.now(), Instant.now())
        );

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(projectRepository.streamAllByAppUser(currentUser))
                .thenReturn(projects.stream());

        when(projectDtoMapper.createProjectDto(projects.get(0)))
                .thenReturn(projectsDto.get(0));

        when(projectDtoMapper.createProjectDto(projects.get(1)))
                .thenReturn(projectsDto.get(1));

        List<ProjectDto> result = projectController.fetchProjects(Optional.of(prefix));

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("p1", result.get(0).getName());
        assertEquals("p2", result.get(1).getName());

        verify(projectRepository)
                .streamAllByAppUser(currentUser);

        verify(projectDtoMapper, times(2))
                .createProjectDto(any(ProjectEntity.class));
    }

    @Test
    void createProject_successful() {

        String name = "testProject";

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        ProjectEntity project = new ProjectEntity(
                1L,
                name,
                Instant.now(),
                Instant.now(),
                List.of(),
                currentUser
        );

        ProjectDto projectDto = new ProjectDto(
                1L,
                name,
                Instant.now(),
                Instant.now()
        );

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(projectRepository.findByNameAndAppUser(name, currentUser))
                .thenReturn(Optional.empty());

        when(projectRepository.saveAndFlush(any(ProjectEntity.class)))
                .thenReturn(project);

        when(projectDtoMapper.createProjectDto(project))
                .thenReturn(projectDto);

        ProjectDto result = projectController.createProject(name);

        assertNotNull(result);
        assertEquals(projectDto, result);

        verify(projectRepository)
                .findByNameAndAppUser(name, currentUser);

        verify(projectRepository)
                .saveAndFlush(any(ProjectEntity.class));
    }

    @Test
    void createProject_nameCannotBeEmpty_throwsException() {

        String name = "";

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> projectController.createProject(name));

        assertEquals("Name cannot be empty", exception.getMessage());
    }

    @Test
    void createProject_projectAlreadyExists_throwsException() {

        String name = "testProject";

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        ProjectEntity project = new ProjectEntity(
                1L,
                name,
                Instant.now(),
                Instant.now(),
                List.of(),
                currentUser
        );

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(projectRepository.findByNameAndAppUser(name, currentUser))
                .thenReturn(Optional.of(project));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> projectController.createProject(name));

        assertEquals("Project \"testProject\" already exists", exception.getMessage());

        verify(controllerHelper)
                .getCurrentUser();

        verify(projectRepository)
                .findByNameAndAppUser(name, currentUser);
    }

    @Test
    void editProject_successful() {

        Long projectId = 1L;
        String name = "testProject";

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        ProjectEntity project = new ProjectEntity(
                projectId,
                "OldName",
                Instant.now(),
                Instant.now(),
                List.of(),
                currentUser
        );

        ProjectDto projectDto = new ProjectDto(
                projectId,
                name,
                Instant.now(),
                Instant.now()
        );

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(controllerHelper.getProjectOrThrowException(projectId))
                .thenReturn(project);

        when(projectRepository.findByNameAndAppUser(name, currentUser))
                .thenReturn(Optional.empty());

        when(projectRepository.saveAndFlush(any(ProjectEntity.class))).thenAnswer(invocation -> {
            ProjectEntity savedProject = invocation.getArgument(0);
            savedProject.setName(name);
            savedProject.setUpdatedAt(Instant.now());
            return savedProject;
        });

        when(projectDtoMapper.createProjectDto(any(ProjectEntity.class)))
                .thenReturn(projectDto);

        ProjectDto result = projectController.editProject(projectId, name);

        assertNotNull(result);
        assertEquals(projectDto, result);

        verify(controllerHelper)
                .getCurrentUser();

        verify(controllerHelper)
                .getProjectOrThrowException(projectId);

        verify(projectRepository)
                .findByNameAndAppUser(name, currentUser);

        verify(projectRepository)
                .saveAndFlush(argThat(savedProject -> {
                    assertEquals(name, savedProject.getName());
                    return true;
                }));

        verify(projectDtoMapper)
                .createProjectDto(project);
    }

    @Test
    void deleteProject_successful() {

        Long projectId = 1L;

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        ProjectEntity project = new ProjectEntity(
                projectId,
                "name",
                Instant.now(),
                Instant.now(),
                List.of(),
                currentUser
        );

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(controllerHelper.getProjectOrThrowException(projectId))
                .thenReturn(project);

        doNothing().when(projectRepository).deleteById(projectId);

        ResponseEntity<String> response = projectController.deleteProject(projectId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Project with id - \"1\" was successfully deleted", response.getBody());

        verify(controllerHelper)
                .getCurrentUser();

        verify(controllerHelper)
                .getProjectOrThrowException(projectId);

        verify(projectRepository)
                .deleteById(projectId);
    }

    @Test
    void deleteProject_unauthorizedUser_throwsException() {

        Long projectId = 1L;

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1L);

        UserEntity anotherUser = new UserEntity();
        anotherUser.setId(2L);

        ProjectEntity project = new ProjectEntity(
                projectId,
                "TestProject",
                Instant.now(),
                Instant.now(),
                List.of(),
                anotherUser
        );

        when(controllerHelper.getCurrentUser())
                .thenReturn(currentUser);

        when(controllerHelper.getProjectOrThrowException(projectId))
                .thenReturn(project);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> projectController.deleteProject(projectId));

        assertEquals("You do not have permission to delete this project.", exception.getMessage());

        verify(controllerHelper)
                .getCurrentUser();

        verify(controllerHelper)
                .getProjectOrThrowException(projectId);

        verify(projectRepository, times(0));
    }

    @Test
    void deleteProject_projectNotFound_throwsException() {

        Long projectId = 1L;

        when(controllerHelper.getCurrentUser())
                .thenReturn(new UserEntity());

        when(controllerHelper.getProjectOrThrowException(projectId))
                .thenThrow(new BadRequestException("Project not found"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> projectController.deleteProject(projectId));

        assertEquals("Project not found", exception.getMessage());

        verify(controllerHelper)
                .getCurrentUser();

        verify(controllerHelper)
                .getProjectOrThrowException(projectId);

        verify(projectRepository, times(0));
    }
}
