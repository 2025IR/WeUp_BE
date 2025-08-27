package com.example.weup.validate;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Member;
import com.example.weup.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatValidator {

    private final ChatRoomRepository chatRoomRepository;

    private final MemberValidator memberValidator;

    public ChatRoom validateChatRoom(Long chatRoomId) {
        log.debug("chat validator - validate chat room IN, chat room id : {}", chatRoomId);
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));
    }

    public Member validateMemberInChatRoomSession(Long chatRoomId, Long userId) {
        log.debug("chat validator - validate member in chat room session IN");
        ChatRoom chatRoom = validateChatRoom(chatRoomId);
        return memberValidator.validateActiveMemberInProject(userId, chatRoom.getProject().getProjectId());
    }

}
