package org.example.api.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.api.controllers.helpers.ControllerHelper;
import org.example.api.dto.ProjectDto;
import org.example.api.exeptions.BadRequestException;
import org.example.api.mappers.ProjectDtoMapper;
import org.example.store.entities.ProjectEntity;
import org.example.store.entities.UserEntity;
import org.example.store.repositories.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Transactional
@RestController
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectDtoMapper projectDtoMapper;
    private final ControllerHelper controllerHelper;

    public static final String FETCH_PROJECT = "/api/projects";
    public static final String CREATE_PROJECT = "/api/projects";
    public static final String EDIT_PROJECT = "/api/projects/{project_id}";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";

    @GetMapping(FETCH_PROJECT)
    public List<ProjectDto> fetchProjects(
            @RequestParam(value = "prefix_name", required = false) Optional<String> prefixName
    ) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        prefixName = prefixName.filter(name -> !name.trim().isEmpty());

        final Stream<ProjectEntity> projectStream = prefixName
                .map(name -> projectRepository.streamAllByNameStartsWithIgnoreCaseAndAppUser(name, currentUser))
                .orElseGet(() -> projectRepository.streamAllByAppUser(currentUser));

        return projectStream
                .map(projectDtoMapper::createProjectDto)
                .collect(Collectors.toList());
    }


    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(@RequestParam String name) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name cannot be empty");
        }

        projectRepository.findByNameAndAppUser(name, currentUser).ifPresent(project -> {
            throw new BadRequestException(String.format("Project \"%s\" already exists", name));
        });

        final ProjectEntity project = projectRepository.saveAndFlush(
                ProjectEntity.builder()
                        .name(name)
                        .appUser(currentUser)
                        .build()
        );

        return projectDtoMapper.createProjectDto(project);
    }

    @PatchMapping(EDIT_PROJECT)
    public ProjectDto editProject(@PathVariable("project_id") Long projectId, @RequestParam String name) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name cannot be empty");
        }

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        if (!Objects.equals(project.getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to edit this project.");
        }

        projectRepository.findByNameAndAppUser(name, currentUser)
                .filter(anotherProject -> !Objects.equals(anotherProject.getId(), projectId))
                .ifPresent(anotherProject -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists", name));
                });

        project.setName(name);
        project = projectRepository.saveAndFlush(project);

        return projectDtoMapper.createProjectDto(project);
    }

    @DeleteMapping(DELETE_PROJECT)
    public ResponseEntity<String> deleteProject(@PathVariable("project_id") Long projectId) {

        final UserEntity currentUser = controllerHelper.getCurrentUser();

        final ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        if (!Objects.equals(project.getAppUser().getId(), currentUser.getId())) {
            throw new BadRequestException("You do not have permission to delete this project.");
        }

        projectRepository.deleteById(projectId);

        return ResponseEntity.ok(String.format("Project with id - \"%s\" was successfully deleted", projectId));
    }

}
