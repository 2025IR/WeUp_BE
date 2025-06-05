package com.example.weup.service;

import com.example.weup.dto.request.AiChatRequestDTO;
import com.example.weup.dto.request.SendMessageRequestDto;
import com.example.weup.dto.response.ReceiveMessageResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final RestTemplate restTemplate;

    private final ChatService chatService;

    private final SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    public void sendMessageToAi(Long roomId, AiChatRequestDTO aiChatRequestDTO) throws JsonProcessingException {

        SendMessageRequestDto sendMessageRequestDto = SendMessageRequestDto.builder()
                .senderId(aiChatRequestDTO.getSenderId())
                .message(aiChatRequestDTO.getUserInput())
                .build();

        ReceiveMessageResponseDto requestMessage = chatService.saveChatMessage(roomId, sendMessageRequestDto);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, requestMessage);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> jsonBody = new HashMap<>();
            jsonBody.put("user_input", aiChatRequestDTO.getUserInput());
            jsonBody.put("project_id", aiChatRequestDTO.getProjectId());

            log.debug("jsonBody: {}", jsonBody);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(aiServerUrl, requestEntity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String realMessage = root.get("response").asText();

            log.debug("response: {}", realMessage);

            SendMessageRequestDto responseData = SendMessageRequestDto.builder()
                    .senderId(1L)
                    .message(realMessage)
                    .build();

            ReceiveMessageResponseDto responseMessage = chatService.saveChatMessage(roomId, responseData);
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, responseMessage);

        } catch (RestClientException e) {
            throw new RuntimeException("AI Chat 서버 요청 중 오류 발생 : " + e);
        }
    }

}
