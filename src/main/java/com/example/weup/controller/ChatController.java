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

        ReceiveMessageResponseDto receiveMessage = chatService.saveChatMessage(roomId, messageDto);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, receiveMessage);
    }

    @ResponseBody
    @PostMapping("/send/image")
    public void sendImageMessage(HttpServletRequest request,
                                 @ModelAttribute SendImageMessageRequestDTO sendImageMessageRequestDTO) throws IOException {

        jwtUtil.resolveToken(request);

        chatService.handleImageMessage(sendImageMessageRequestDTO);
    }

    @ResponseBody
    @PostMapping("/chat/messages/{roomId}")
    public ResponseEntity<DataResponseDTO<ChatPageResponseDto>> getChatMessages(HttpServletRequest request,
                                                                                @PathVariable Long roomId,
                                                                                @RequestBody GetPageable pageable) throws JsonProcessingException {

        jwtUtil.resolveToken(request);

        ChatPageResponseDto data = chatService.getChatMessages(roomId, pageable.getPage(), pageable.getSize());

        return ResponseEntity.ok(DataResponseDTO.of(data, "채팅 내역 조회가 완료되었습니다."));
    }
}
