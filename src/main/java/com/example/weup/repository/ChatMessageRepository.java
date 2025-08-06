package com.example.weup.repository;

import com.example.weup.entity.ChatMessage;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Member;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoom_ChatRoomId(Long chatRoomId, PageRequest pageRequest);

    void deleteByChatRoom(ChatRoom chatRoom);

    ChatMessage findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(Long roomId);

    List<ChatMessage> findByMember(Member member);
}
