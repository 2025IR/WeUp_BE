package com.example.weup.repository;

import com.example.weup.entity.Role;
import com.example.weup.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByRoleName(String roleName);
    Optional<Role> findByProjectAndRoleName(Project project, String roleName);
    Optional<Role> findByRoleName(String roleName);
}
