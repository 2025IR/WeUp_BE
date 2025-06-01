package com.example.weup.controller;

import com.example.weup.dto.request.AiChatRequestDTO;
import com.example.weup.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<String> sendMessageToAi(AiChatRequestDTO aiChatRequestDTO) {

        String result = aiChatService.sendMessageToAi(aiChatRequestDTO);

        return ResponseEntity.ok().body(result);
    }
}
