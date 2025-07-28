package com.example.weup.repository;

import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.ChatRoomMember;
import com.example.weup.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    List<ChatRoomMember> findByChatRoom(ChatRoom chatRoom);

    boolean existsByChatRoomAndMember(ChatRoom chatRoom, Member member);

    ChatRoomMember findByChatRoomAndMember(ChatRoom chatRoom, Member member);
}
