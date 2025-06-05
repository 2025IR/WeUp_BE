package com.example.weup.controller;

import com.example.weup.dto.request.AiChatRequestDTO;
import com.example.weup.service.AiChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat/{roomId}")
    public void sendMessageToAi(@PathVariable Long roomId, @RequestBody AiChatRequestDTO aiChatRequestDTO) throws JsonProcessingException {

        log.debug("sendMessageToAi, {}, {}", aiChatRequestDTO.getUserInput(), aiChatRequestDTO.getProjectId());

        aiChatService.sendMessageToAi(roomId, aiChatRequestDTO);
    }
}
