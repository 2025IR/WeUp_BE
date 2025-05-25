package com.example.weup.controller;

import com.example.weup.dto.request.ChatMessageRequestDto;
import com.example.weup.dto.response.ChatMessageResponseDto;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final JwtUtil jwtUtil;

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/send/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageRequestDto messageDto) throws JsonProcessingException {
        chatService.saveChatMessage(roomId, messageDto);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, messageDto);
    }

    @PostMapping("/send/{roomId}/{projectId}/image")
    public void sendImageMessage(
            HttpServletRequest request,
            @PathVariable Long roomId,
            @PathVariable Long projectId,
            @ModelAttribute MultipartFile file) throws IOException {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        chatService.handleImageMessage(projectId, roomId, userId, file);
    }



    @ResponseBody
    @GetMapping("/chat/{roomId}/messages")
    public ResponseEntity<DataResponseDTO<Page<ChatMessageResponseDto>>> getChatMessages(@PathVariable Long roomId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) throws JsonProcessingException {
        Page<ChatMessageResponseDto> messages = chatService.getChatMessages(roomId, page, size);
        return ResponseEntity
                .ok()
                .body(DataResponseDTO.of(messages, "채팅 내역 조회 완료"));
    }
}
