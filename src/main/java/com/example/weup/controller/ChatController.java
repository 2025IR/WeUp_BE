package com.example.weup.controller;

import com.example.weup.dto.request.ChatMessageRequestDto;
import com.example.weup.dto.response.ChatMessageResponseDto;
import com.example.weup.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageRequestDto messageDto) throws JsonProcessingException {
        chatService.saveChatMessage(roomId, messageDto);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, messageDto);
    }

    @GetMapping("/api/chat/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageResponseDto>> getChatMessages(@PathVariable Long roomId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) throws JsonProcessingException {
        Page<ChatMessageResponseDto> messages = chatService.getChatMessages(roomId, page, size);
        return ResponseEntity.ok(messages);
    }
}
