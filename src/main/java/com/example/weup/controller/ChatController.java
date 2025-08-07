package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.*;
import com.example.weup.dto.response.*;
import com.example.weup.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/send/{chatRoomId}")
    public void sendMessage(@DestinationVariable Long chatRoomId, SendMessageRequestDTO messageDto) throws JsonProcessingException {

        log.info("요청자 : {}, websocket send chatting -> start", messageDto.getSenderId());

        chatService.sendBasicMessage(chatRoomId, messageDto);
        log.info("요청자 : {}, websocket send chatting -> success", messageDto.getSenderId());
    }

    @ResponseBody
    @PostMapping("/chat/send/image/{chatRoomId}")  // chatRoomId 밖으로 빼기
    public void sendImageMessage(@LoginUser Long userId, @PathVariable Long chatRoomId,
                                 @ModelAttribute SendImageMessageRequestDTO sendImageMessageRequestDTO) throws IOException {

        log.info("요청자 : {}, send image chatting -> start", userId);
        chatService.sendImageMessage(chatRoomId, sendImageMessageRequestDTO);

        log.info("요청자 : {}, send image chatting -> success", userId);
    }

    @ResponseBody
    @PostMapping("/chat/messages/{chatRoomId}")
    public ResponseEntity<DataResponseDTO<Slice<ReceiveMessageResponseDTO>>> getChatMessages(@LoginUser Long userId,
                                                                  @PathVariable Long chatRoomId,
                                                                  @RequestBody GetPageable pageable) throws JsonProcessingException {

        log.info("요청자 : {}, get chatting messages -> start", userId);
        Slice<ReceiveMessageResponseDTO> data = chatService.getChatMessages(chatRoomId, pageable);

        log.info("요청자 : {}, get chatting messages -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of(data, "채팅 내역 조회가 완료되었습니다."));
    }

}
