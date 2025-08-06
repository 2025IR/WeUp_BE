package com.example.weup.controller;

import com.example.weup.dto.request.AiChatRequestDTO;
import com.example.weup.dto.request.AiMinutesCreateRequestDTO;
import com.example.weup.dto.request.AiRoleAssignRequestDTO;
import com.example.weup.dto.request.AiTodoCreateRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
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

        log.info("요청자 : {}, send message to ai -> start", aiChatRequestDTO.getSenderId());
        aiChatService.sendMessageToAi(roomId, aiChatRequestDTO);

        log.info("요청자 : {}, send message to ai -> success", aiChatRequestDTO.getSenderId());
    }

    @PostMapping("/role/assign")
    public ResponseEntity<DataResponseDTO<String>> aiAssignRole(@RequestBody AiRoleAssignRequestDTO aiRoleAssignDto) {

        log.info("요청자 : Flask Server, AI Assign Role -> start");
        aiChatService.aiAssignRole(aiRoleAssignDto);

        log.info("요청자 : Flask Server, AI Assign Role -> success");
        return ResponseEntity.ok(DataResponseDTO.of("AI 비서 - 역할 변경이 완료되었습니다."));
    }

    @PostMapping("/todo/create")
    public ResponseEntity<DataResponseDTO<String>> aiTodoCreate(@RequestBody AiTodoCreateRequestDTO aiTodoCreateDto) {

        log.info("요청자 : Flask Server, AI Todo Create -> start");
        aiChatService.aiTodoCreate(aiTodoCreateDto);

        log.info("요청자 : Flask Server, AI Todo Create -> success");
        return ResponseEntity.ok(DataResponseDTO.of("AI 비서 - Todo 생성이 완료되었습니다."));
    }

    @PostMapping("/board/minutes/create")
    public ResponseEntity<DataResponseDTO<String>> aiCreateMinutes(@RequestBody AiMinutesCreateRequestDTO aiMinutesCreateRequestDTO) {

        log.info("요청자 : Flask Server, AI Create Minutes -> start");
        aiChatService.aiCreateMinutes(aiMinutesCreateRequestDTO);

        log.info("요청자 : Flask Server, AI Create Minutes -> success");
        return ResponseEntity.ok(DataResponseDTO.of("AI 비서 - Todo 생성이 완료되었습니다."));
    }
}
