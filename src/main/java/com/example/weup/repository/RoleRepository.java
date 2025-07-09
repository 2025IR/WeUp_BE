package com.example.weup.repository;

import com.example.weup.entity.Role;
import com.example.weup.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByProjectAndRoleName(Project project, String roleName);

    List<Role> findAllByProject(Project project);

    List<Role> findByProjectAndRoleIdIn(Project project, List<Long> roleIds);

    void deleteByProject(Project project);
}
