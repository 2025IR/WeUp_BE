package com.example.weup.controller;

import com.example.weup.dto.request.GetPageable;
import com.example.weup.dto.request.SendImageMessageRequestDTO;
import com.example.weup.dto.request.SendMessageRequestDto;
import com.example.weup.dto.response.ChatPageResponseDto;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.dto.response.ReceiveMessageResponseDto;
import com.example.weup.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final JwtUtil jwtUtil;

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/send/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, SendMessageRequestDto messageDto) throws JsonProcessingException {

        log.debug("send message controller : {}, {}, {}", messageDto.getSenderId(), messageDto.getMessage(), messageDto.getSentAt());

        ReceiveMessageResponseDto receiveMessage = chatService.saveChatMessage(roomId, messageDto);

        log.debug("send message controller receiveMessage Object @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.debug("{}, {}, {}, {}, {}", receiveMessage.getSenderId(), receiveMessage.getSenderName(), receiveMessage.getSenderProfileImage(), receiveMessage.getSentAt(), receiveMessage.getMessage());

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, receiveMessage);
    }

    @PostMapping("/send/image")
    @ResponseBody
    public void sendImageMessage(
            HttpServletRequest request,
            @ModelAttribute SendImageMessageRequestDTO sendImageMessageRequestDTO) throws IOException {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        chatService.handleImageMessage(sendImageMessageRequestDTO);
    }

    @ResponseBody
    @PostMapping("/chat/messages/{roomId}")
    public ResponseEntity<DataResponseDTO<ChatPageResponseDto>> getChatMessages(HttpServletRequest request,
                                                                                @PathVariable Long roomId,
                                                                                @RequestBody GetPageable pageable) throws JsonProcessingException {
        log.debug("\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.debug("get chat messages controller 진입 여부 확인 : " + pageable.getPage() + ", " + pageable.getSize());

        jwtUtil.resolveToken(request);

        ChatPageResponseDto data = chatService.getChatMessages(roomId, pageable.getPage(), pageable.getSize());
        return ResponseEntity
                .ok()
                .body(DataResponseDTO.of(data, "채팅 내역 조회 완료"));
    }
}
