package com.example.weup.repository;

import com.example.weup.entity.Member;
import com.example.weup.entity.MemberRole;
import com.example.weup.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRoleRepository extends JpaRepository<MemberRole, Long> {

    @Query("SELECT mr FROM MemberRole mr " +
            "JOIN FETCH mr.member m " +
            "JOIN FETCH mr.role r " +
            "WHERE m.project.projectId = :projectId")
    List<MemberRole> findAllByProjectId(@Param("projectId") Long projectId);

    @Modifying
    void deleteByRole(Role role);

    void deleteByMember(Member member);

    List<MemberRole> findByMember(Member member);

    List<MemberRole> findAllByMember_MemberId(Long memberId);
}