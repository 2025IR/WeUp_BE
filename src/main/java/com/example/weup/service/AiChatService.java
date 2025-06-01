package com.example.weup.service;

import com.example.weup.dto.request.AiChatRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final RestTemplate restTemplate;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    public String sendMessageToAi(AiChatRequestDTO aiChatRequestDTO) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(aiServerUrl, aiChatRequestDTO, String.class);

            return response.getBody();
        } catch (RestClientException e) {
            throw new RuntimeException("AI Chat 서버 요청 중 오류 발생 : " + e);
        }
    }

}
