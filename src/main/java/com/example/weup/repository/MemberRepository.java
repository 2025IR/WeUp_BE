package com.example.weup.repository;

import com.example.weup.entity.MemberEntity;
import com.example.weup.entity.ProjectEntity;
import com.example.weup.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    Optional<MemberEntity> findByUserAndProject(UserEntity user, ProjectEntity project);
    
    boolean existsByUserAndProject(UserEntity user, ProjectEntity project);
    
    List<MemberEntity> findByUser(UserEntity user);
    
    List<MemberEntity> findByProject(ProjectEntity project);
} 