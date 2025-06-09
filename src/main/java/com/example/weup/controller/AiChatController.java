package com.example.weup.controller;

import com.example.weup.dto.request.AiChatRequestDTO;
import com.example.weup.dto.request.AiRoleAssignRequestDTO;
import com.example.weup.dto.request.AiTodoCreateRequestDTO;
import com.example.weup.service.AiChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/role/assign")
    public ResponseEntity<String> aiAssignRole(@RequestBody AiRoleAssignRequestDTO aiRoleAssignDto) {

        log.debug("aiAssignRole Controller, {}, {}, {}", aiRoleAssignDto.getProjectId(), aiRoleAssignDto.getUserName(), aiRoleAssignDto.getRoleName());

        aiChatService.aiAssignRole(aiRoleAssignDto);

        return ResponseEntity.ok().body("ai assign role success");
    }

    @PostMapping("/todo/create")
    public ResponseEntity<String> aiTodoCreate(@RequestBody AiTodoCreateRequestDTO aiTodoCreateDto) {

        log.debug("aiTodoCreate Controller, {}, {}", aiTodoCreateDto.getTodoName(), aiTodoCreateDto.getStartDate());

        aiChatService.aiTodoCreate(aiTodoCreateDto);

        return ResponseEntity.ok().body("ai todo create success");
    }

}
