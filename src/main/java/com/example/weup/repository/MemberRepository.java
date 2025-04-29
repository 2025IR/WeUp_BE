package com.example.weup.repository;

import com.example.weup.entity.Member;
import com.example.weup.entity.Project;
import com.example.weup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByUserAndProject(User user, Project project);

    List<Member> findByProject_ProjectIdAndIsMemberDeletedFalse(Long projectId);

    Optional<Member> findByUserAndProject(User user, Project project);
} 