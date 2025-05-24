package com.example.weup.repository;

import com.example.weup.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByProject_ProjectId(Long projectId);
}
