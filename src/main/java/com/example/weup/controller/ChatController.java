package com.example.weup.controller;

import com.example.weup.dto.request.SendMessageRequestDto;
import com.example.weup.dto.response.ChatPageResponseDto;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.ReceiveMessageResponseDto;
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

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/send/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, SendMessageRequestDto messageDto) throws JsonProcessingException {

        log.debug("send message controller : {}, {}, {}", messageDto.getSenderId(), messageDto.getMessage(), messageDto.getSentAt());

        ReceiveMessageResponseDto receiveMessage = chatService.saveChatMessage(roomId, messageDto);

        log.debug("send message controller receiveMessage Object @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.debug("{}, {}, {}, {}, {}", receiveMessage.getSenderId(), receiveMessage.getSenderName(), receiveMessage.getSenderProfileImage(), receiveMessage.getSentAt(), receiveMessage.getMessage());

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, receiveMessage);
    }

    @ResponseBody
    @GetMapping("/chat/messages/{roomId}")
    public ResponseEntity<DataResponseDTO<ChatPageResponseDto>> getChatMessages(@PathVariable Long roomId,
                                                                                @RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "20") int size) throws JsonProcessingException {
        ChatPageResponseDto data = chatService.getChatMessages(roomId, page, size);
        return ResponseEntity
                .ok()
                .body(DataResponseDTO.of(data, "채팅 내역 조회 완료"));
    }
}
