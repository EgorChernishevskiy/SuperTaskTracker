package org.example.api.controllers.helpers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.api.exeptions.NotFoundException;
import org.example.store.entities.ProjectEntity;
import org.example.store.entities.TaskEntity;
import org.example.store.entities.TaskStateEntity;
import org.example.store.entities.UserEntity;
import org.example.store.repositories.ProjectRepository;
import org.example.store.repositories.TaskRepository;
import org.example.store.repositories.TaskStateRepository;
import org.example.store.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Transactional
public class ControllerHelper {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskStateRepository taskStateRepository;

    public ProjectEntity getProjectOrThrowException(Long projectId) {

        return projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format(
                                        "Project with \"%s\" doesn't exist.",
                                        projectId
                                )
                        )
                );
    }

    public TaskStateEntity getTaskStateIdOrThrowException(Long taskStateId) {

        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Task state \"%s\" doesn't exist.", taskStateId)));
    }

    public TaskEntity getTaskIdOrThrowException(Long taskId) {

        return taskRepository
                .findById(taskId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Task \"%s\" doesn't exist.", taskId)));
    }

    public UserEntity getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}