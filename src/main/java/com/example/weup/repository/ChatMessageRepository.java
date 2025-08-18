package com.example.weup.repository;

import com.example.weup.entity.ChatMessage;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Member;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query(value = "SELECT * FROM chat_message WHERE chat_room_id = :chatRoomId" +
            " ORDER BY sent_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<ChatMessage> findMessagesByChatRoomIdWithOffset(@Param("chatRoomId") Long chatRoomId, @Param("limit") int limit, @Param("offset") int offset);

    void deleteByChatRoom(ChatRoom chatRoom);

    ChatMessage findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(Long roomId);

    List<ChatMessage> findByMember(Member member);

    List<Long> findMessageIdByChatRoom_ChatRoomIdAndSentAtAfter(Long chatRoomId, LocalDateTime sentAt);

    long countByChatRoom_ChatRoomIdAndSentAtAfter(Long chatRoomId, LocalDateTime lastDateTime);
}
