package com.example.weup.repository;

import com.example.weup.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByProjectDeletedTimeBefore(LocalDateTime localDateTime);

    List<Project> findByProjectDeletedTimeIsNotNull();
} 