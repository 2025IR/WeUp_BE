package com.example.weup.repository;

import com.example.weup.entity.Member;
import com.example.weup.entity.Member_Role;
import com.example.weup.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRoleRepository extends JpaRepository<Member_Role, Long> {
    // 프로젝트 내 모든 멤버별 역할을 한 번에 조회 - n대m 관계를 피하기 위해 사용
    @Query("SELECT mr FROM Member_Role mr " +
            "JOIN FETCH mr.member m " +
            "JOIN FETCH mr.role r " +
            "WHERE m.project.projectId = :projectId")
    List<Member_Role> findAllByProjectId(@Param("projectId") Long projectId);

    boolean existsByMemberAndRole(Member member, Role role);

    Optional<Member_Role> findByMemberAndRole(Member member, Role role);

    boolean existsByRole(Role role);
}