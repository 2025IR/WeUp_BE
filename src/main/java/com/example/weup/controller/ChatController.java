package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.*;
import com.example.weup.dto.response.ChatPageResponseDto;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetChatRoomListDTO;
import com.example.weup.dto.response.ReceiveMessageResponseDto;
import com.example.weup.entity.User;
import com.example.weup.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final SimpMessagingTemplate messagingTemplate;

    @ResponseBody
    @PostMapping("/chat/create")
    public ResponseEntity<DataResponseDTO<String>> createChatRoom(@LoginUser User user,
                                                                  @RequestBody CreateChatRoomDTO createChatRoomDto) {

        chatService.createChatRoom(user, createChatRoomDto);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방 생성이 완료되었습니다."));
    }

    @ResponseBody
    @PostMapping("/chat/invite/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<String>> inviteChatRoom(@LoginUser User user,
                                                                  @PathVariable Long chatRoomId,
                                                                  @RequestBody InviteChatRoomDTO inviteChatRoomDTO) {

        chatService.inviteChatMember(chatRoomId, inviteChatRoomDTO);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방에 초대되었습니다."));
    }

    @ResponseBody
    @PostMapping("/chat/edit/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<String>> editChatRoom(@LoginUser User user,
                                                                @PathVariable Long chatRoomId,
                                                                @RequestBody String chatRoomName) {

        chatService.editChatRoomName(chatRoomId, chatRoomName);

        return ResponseEntity.ok(DataResponseDTO.of("채팅방 이름이 수정되었습니다."));
    }

    @ResponseBody
    @PostMapping("/chat/list/{projectId}")
    public ResponseEntity<DataResponseDTO<List<GetChatRoomListDTO>>> listChatRoom(@LoginUser User user,
                                                                             @PathVariable Long projectId) {

        List<GetChatRoomListDTO> data = chatService.getChatRoomList(user, projectId);

        return ResponseEntity.ok(DataResponseDTO.of(data, "채팅방 리스트를 불러왔습니다."));
    }

    @MessageMapping("/send/{chatRoomId}")
    public void sendMessage(@DestinationVariable Long chatRoomId, SendMessageRequestDTO messageDto) throws JsonProcessingException {

        log.info("요청자 : {}, websocket send chatting -> start", messageDto.getSenderId());

        ReceiveMessageResponseDto receiveMessage = chatService.saveChatMessage(chatRoomId, messageDto);
        log.info("요청자 : {}, websocket send chatting -> success", messageDto.getSenderId());

        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, receiveMessage);
    }

    @ResponseBody
    @PostMapping("/send/image")
    public void sendImageMessage(@LoginUser Long userId,
                                 @ModelAttribute SendImageMessageRequestDTO sendImageMessageRequestDTO) throws IOException {

        log.info("요청자 : {}, send image chatting -> start", userId);
        chatService.handleImageMessage(sendImageMessageRequestDTO);

        log.info("요청자 : {}, send image chatting -> success", userId);
    }

    @ResponseBody
    @PostMapping("/chat/messages/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<ChatPageResponseDto>> getChatMessages(@LoginUser Long userId,
                                                                                @PathVariable Long chatRoomId,
                                                                                @RequestBody GetPageable pageable) throws JsonProcessingException {

        log.info("요청자 : {}, get chatting messages -> start", userId);
        ChatPageResponseDto data = chatService.getChatMessages(chatRoomId, pageable.getPage(), pageable.getSize());

        log.info("요청자 : {}, get chatting messages -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of(data, "채팅 내역 조회가 완료되었습니다."));
    }
}
