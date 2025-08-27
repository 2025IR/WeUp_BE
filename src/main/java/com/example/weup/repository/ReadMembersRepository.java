package com.example.weup.repository;

import com.example.weup.entity.ReadMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadMembersRepository extends JpaRepository<ReadMembers, Long> {

    @Query("SELECT COUNT(mr) FROM ReadMembers mr WHERE mr.chatMessage.messageId = :messageId")
    Long countReadMembersByMessageId(@Param("messageId") Long messageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT IGNORE INTO read_members(message_id, member_id) VALUES (:messageId, :memberId)", nativeQuery = true)
    int insertIgnore(@Param("messageId") Long messageId, @Param("memberId") Long memberId);
}
