package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.*;
import com.example.weup.constant.SenderType;
import com.example.weup.dto.response.ReceiveMessageResponseDTO;
import com.example.weup.dto.response.RedisMessageDTO;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final RestTemplate restTemplate;

    private final ChatService chatService;

    private final ObjectMapper objectMapper;

    private final MemberRepository memberRepository;

    private final MemberRoleRepository memberRoleRepository;

    private final RoleRepository roleRepository;

    private final TodoRepository todoRepository;

    private final TagRepository tagRepository;

    private final BoardRepository boardRepository;

    private final ProjectValidator projectValidator;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    public void sendMessageToAi(Long chatRoomId, AiChatRequestDTO aiChatRequestDTO) throws JsonProcessingException {

        SendMessageRequestDTO sendMessageRequestDto = SendMessageRequestDTO.builder()
                .senderId(aiChatRequestDTO.getSenderId())
                .message(aiChatRequestDTO.getUserInput())
                .build();

        Member sendMember = memberRepository.findById(aiChatRequestDTO.getSenderId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

        chatService.sendBasicMessage(chatRoomId, sendMessageRequestDto);

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

            chatService.sendAIMessage(chatRoomId, realMessage, aiChatRequestDTO.getUserInput(), sendMember.getUser().getName());

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

    @Transactional
    public void aiCreateMinutes(AiMinutesCreateRequestDTO aiMinutesCreateRequestDTO) {

        Project project = projectValidator.validateActiveProject(aiMinutesCreateRequestDTO.getProjectId());

        Tag tag = tagRepository.findByTagName("회의록")
                .orElseThrow(() -> new GeneralException(ErrorInfo.TAG_NOT_FOUND));

        Board board = Board.builder()
                .project(project)
                .tag(tag)
                .member(null)
                .title(aiMinutesCreateRequestDTO.getTitle())
                .contents(aiMinutesCreateRequestDTO.getContents())
                .boardCreateTime(LocalDateTime.now())
                .senderType(SenderType.AI)
                .build();

        boardRepository.save(board);
    }
}
