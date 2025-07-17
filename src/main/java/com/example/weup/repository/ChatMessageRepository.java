package com.example.weup.repository;

import com.example.weup.entity.ChatMessage;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoom_ChatRoomId(Long chatRoomId);

    void deleteByChatRoom(ChatRoom chatRoom);

    List<ChatMessage> findByUser(User user);
}
