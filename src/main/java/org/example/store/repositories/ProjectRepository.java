package org.example.store.repositories;

import org.example.store.entities.ProjectEntity;
import org.example.store.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.stream.Stream;

    public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

        Optional<ProjectEntity> findByName(String name);

        Stream<ProjectEntity> streamAllBy();

        Stream<ProjectEntity> streamAllByNameStartsWithIgnoreCase(String prefixName);

        Stream<ProjectEntity> streamAllByNameStartsWithIgnoreCaseAndAppUser(String prefixName, UserEntity appUser);

        Stream<ProjectEntity> streamAllByAppUser(UserEntity appUser);

        Optional<ProjectEntity> findByNameAndAppUser(String name, UserEntity appUser);
    }
