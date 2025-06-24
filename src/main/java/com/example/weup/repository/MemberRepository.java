package com.example.weup.repository;

import com.example.weup.entity.Member;
import com.example.weup.entity.Project;
import com.example.weup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByUserAndProject(User user, Project project);

    List<Member> findByProject_ProjectIdAndIsMemberDeletedFalse(Long projectId);

    Optional<Member> findByUserAndProject(User user, Project project);

    @Query("SELECT member FROM Member member JOIN FETCH member.project project " +
            "WHERE member.user.userId = :userId AND member.isMemberDeleted = false")
    List<Member> findActiveMemberByUserId(@Param("userId") Long userId);

    boolean existsByUser_UserIdAndProject_ProjectId(Long userId, Long projectId);

    Optional<Member> findByUser_UserIdAndProject_ProjectId(Long userId, Long projectId);

    Member findByUser_NameAndProject_ProjectId(String userName, Long projectId);

    List<Member> findAllByUser_UserId(Long userId);
}