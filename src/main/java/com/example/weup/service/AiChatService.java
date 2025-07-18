package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.AiChatRequestDTO;
import com.example.weup.dto.request.AiRoleAssignRequestDTO;
import com.example.weup.dto.request.AiTodoCreateRequestDTO;
import com.example.weup.dto.request.SendMessageRequestDto;
import com.example.weup.dto.response.ReceiveMessageResponseDto;
import com.example.weup.entity.*;
import com.example.weup.repository.*;
import com.example.weup.validate.ProjectValidator;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final RestTemplate restTemplate;

    private final ChatService chatService;

    private final SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper;

    private final MemberRepository memberRepository;

    private final MemberRoleRepository memberRoleRepository;

    private final RoleRepository roleRepository;

    private final TodoRepository todoRepository;

    private final ProjectValidator projectValidator;

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

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(jsonBody, headers);

            log.info("send message to ai -> POST Request To AI Flask Server start : url - {}", aiServerUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(aiServerUrl, requestEntity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String realMessage = root.get("response").asText();

            if (Objects.equals(realMessage, "")) {
                throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
            }
            log.info("send message to ai -> POST Request To AI Flask Server success : message - {}", realMessage);

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

    @Transactional
    public void aiAssignRole(AiRoleAssignRequestDTO aiRoleAssignDto) {

        Project project = projectValidator.validateActiveProject(aiRoleAssignDto.getProjectId());

        Member member = memberRepository.findByUser_NameAndProject_ProjectId(aiRoleAssignDto.getUserName(), project.getProjectId());

        Role role = roleRepository.findByProjectAndRoleName(project, aiRoleAssignDto.getRoleName())
                        .orElseThrow(() -> new GeneralException(ErrorInfo.ROLE_NOT_FOUND));

        memberRoleRepository.deleteByMember(member);

        MemberRole memberRole = MemberRole.builder()
                .member(member)
                .role(role)
                .build();

        memberRoleRepository.save(memberRole);
        log.info("AI Request Assign Role -> success : member id - {}, role id - {}", member.getMemberId(), role.getRoleId());
    }

    @Transactional
    public void aiTodoCreate(AiTodoCreateRequestDTO aiTodoCreateDto) {

        Project project = projectValidator.validateActiveProject(aiTodoCreateDto.getProjectId());

        Todo todo = Todo.builder()
                .project(project)
                .todoName(aiTodoCreateDto.getTodoName())
                .startDate(aiTodoCreateDto.getStartDate())
                .build();

        todoRepository.save(todo);
        log.info("AI Request Todo Create -> success : project id - {}, todo id - {}", project.getProjectId(), todo.getTodoId());
    }

}
