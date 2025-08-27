package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.*;
import com.example.weup.constant.SenderType;
import com.example.weup.dto.response.RedisMessageDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.*;
import com.example.weup.validate.ChatValidator;
import com.example.weup.validate.ProjectValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final RestTemplate restTemplate;

    private final SimpMessagingTemplate messagingTemplate;

    private final ChatService chatService;

    private final ObjectMapper objectMapper;

    private final MemberRepository memberRepository;

    private final MemberRoleRepository memberRoleRepository;

    private final RoleRepository roleRepository;

    private final TodoRepository todoRepository;

    private final TagRepository tagRepository;

    private final BoardRepository boardRepository;

    private final ProjectValidator projectValidator;

    private final StringRedisTemplate redisTemplate;

    private final ChatValidator chatValidator;

    private final ChatMessageRepository chatMessageRepository;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    public void sendMessageToAi(Long chatRoomId, AiChatRequestDTO aiChatRequestDTO) throws JsonProcessingException {

        SendMessageRequestDTO sendMessageRequestDto = SendMessageRequestDTO.builder()
                .senderId(aiChatRequestDTO.getSenderId())
                .message(aiChatRequestDTO.getUserInput())
                .build();

        log.debug("ai chat service - memberId:{}", aiChatRequestDTO.getSenderId());
        Member sendMember = memberRepository.findById(aiChatRequestDTO.getSenderId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

        log.debug("chat service - send basic message로 이동");
        chatService.sendBasicMessage(chatRoomId, sendMessageRequestDto);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> jsonBody = new HashMap<>();
            jsonBody.put("user_input", aiChatRequestDTO.getUserInput());
            jsonBody.put("project_id", String.valueOf(aiChatRequestDTO.getProjectId()));
            jsonBody.put("chat_room_id", String.valueOf(chatRoomId));
            jsonBody.put("mode", "auto");

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(jsonBody, headers);

            log.info("send message to ai -> POST Request To AI Flask Server start : url - {}", aiServerUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(aiServerUrl, requestEntity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            log.debug("send message to ai -> response data check, root : {}", root.toString());

            String route = root.get("route").asText();
            if (route.equals("chat")) {
                String realMessage = root.get("output").asText();
                log.debug("send message to ai -> response data check, realMessage : {}", realMessage);

                if (Objects.equals(realMessage, "")) {
                    throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
                }
                log.info("send message to ai -> POST Request To AI Flask Server success : message - {}", realMessage);

                chatService.sendAIMessage(chatRoomId, realMessage, aiChatRequestDTO.getUserInput(), sendMember.getUser().getName());
            }
            else if (route.equals("http")) {
                log.info("AI 서버의 HTTP 요청에 따른 응답 성공");

                JsonNode outputNode = root.get("output");
                if (outputNode.isNull()) {
                    log.debug("output null ERROR");
                    chatService.sendAIMessage(chatRoomId, "오류 발생", aiChatRequestDTO.getUserInput(), sendMember.getUser().getName());
                    return;
                }

                JsonNode toolNode = outputNode.get("tool");
                if (toolNode.asText().equals("change_role")) {
                    chatService.sendAIMessage(chatRoomId, "역할 변경이 완료되었습니다.", aiChatRequestDTO.getUserInput(), sendMember.getUser().getName());
                    log.info("AI 서버의 HTTP 요청에 따른 역할 변경 완료");
                }
                else if (toolNode.asText().equals("todo_create")) {
                    chatService.sendAIMessage(chatRoomId, "투두 생성이 완료되었습니다.", aiChatRequestDTO.getUserInput(), sendMember.getUser().getName());
                    log.info("AI 서버의 HTTP 요청에 따른 투두 생성 완료");
                }
                else if (toolNode.asText().equals("meeting_create")) {
                    chatService.sendAIMessage(chatRoomId, "회의록 작성이 완료되었습니다.", aiChatRequestDTO.getUserInput(), sendMember.getUser().getName());
                    log.info("AI 서버의 HTTP 요청에 따른 회의록 작성 완료");
                }
            }
            else if (route.equals("clarify")){
                String realMessage = root.get("output").asText();

                if (Objects.equals(realMessage, "")) {
                    throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
                    //todo. 변경
                }

                chatService.sendAIMessage(chatRoomId, realMessage, aiChatRequestDTO.getUserInput(), sendMember.getUser().getName());
                log.info("AI 서버에 입력된 정보 부족, message - {}", realMessage);
            }
            else
                log.warn("서버의 잘못된 요청, route : {}", route);


        } catch (RestClientException e) {
            throw new RuntimeException("AI Chat 서버 요청 중 오류 발생 : " + e);
        }
    }

    @Transactional
    public void aiAssignRole(AiRoleAssignRequestDTO aiRoleAssignDto) {

        Project project = projectValidator.validateActiveProject(aiRoleAssignDto.getProjectId());
        log.debug("project validator -> end");

        Member member = memberRepository.findByUser_NameAndProject_ProjectId(aiRoleAssignDto.getUserName(), project.getProjectId());
        log.debug("member validator -> end");

        Role role = roleRepository.findByProjectAndRoleName(project, aiRoleAssignDto.getRoleName())
                        .orElseThrow(() -> new GeneralException(ErrorInfo.ROLE_NOT_FOUND));
        log.debug("role validator -> end");

        memberRoleRepository.deleteByMember(member);
        MemberRole memberRole = MemberRole.builder()
                .member(member)
                .role(role)
                .build();

        memberRoleRepository.save(memberRole);

        messagingTemplate.convertAndSend("/topic/role/" + aiRoleAssignDto.getProjectId(), Map.of("editedBy", "AI 비서"));

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

        messagingTemplate.convertAndSend(
                "/topic/todo/" + aiTodoCreateDto.getProjectId(),
                Map.of("createdBy", "AI 비서")
        );

        log.info("AI Request Todo Create -> success : project id - {}, todo id - {}", project.getProjectId(), todo.getTodoId());
    }

    @Transactional
    public void aiCreateMinutes(AiMinutesCreateRequestDTO aiCreateMinuteDTO) {

        log.debug("project id : {}, title : {}, contents : {}", aiCreateMinuteDTO.getProjectId(), aiCreateMinuteDTO.getTitle(), aiCreateMinuteDTO.getContents());
        Project project = projectValidator.validateActiveProject(aiCreateMinuteDTO.getProjectId());

        Tag tag = tagRepository.findByTagName("회의록")
                .orElseThrow(() -> new GeneralException(ErrorInfo.TAG_NOT_FOUND));

        Board board = Board.builder()
                .project(project)
                .tag(tag)
                .member(null)
                .title(aiCreateMinuteDTO.getTitle())
                .contents(aiCreateMinuteDTO.getContents())
                .senderType(SenderType.AI)
                .build();

        boardRepository.save(board);
    }

    @Transactional
    public String aiGetMessages(AiGetMessageRequestDTO aiGetMsgDTO) throws JsonProcessingException {

        log.debug("start at : {}, end at : {}, chat room id : {}", aiGetMsgDTO.getStartTime(), aiGetMsgDTO.getEndTime(), aiGetMsgDTO.getChatRoomId());
        List<ChatMessage> combinedMessages = new ArrayList<>();
        chatValidator.validateChatRoom(aiGetMsgDTO.getChatRoomId());

        String redisKey = "chat:room:" + aiGetMsgDTO.getChatRoomId();
        long startAt = aiGetMsgDTO.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endAt = aiGetMsgDTO.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Set<String> redisMessages = redisTemplate.opsForZSet().rangeByScore(redisKey, startAt, endAt);
        if (redisMessages != null && !redisMessages.isEmpty()) {
            for (String redisMessageJson : redisMessages) {
                combinedMessages.add(chatService.translateRedisDtoIntoChatMessage(objectMapper.readValue(redisMessageJson, RedisMessageDTO.class)));
            }
        }

        List<ChatMessage> mysqlMessages = chatMessageRepository.findByChatRoom_ChatRoomIdAndSentAtBetweenOrderBySentAtAsc(
                aiGetMsgDTO.getChatRoomId(), aiGetMsgDTO.getStartTime(), aiGetMsgDTO.getEndTime());
        combinedMessages.addAll(mysqlMessages);

        if (combinedMessages.isEmpty()) {
            log.debug("AI Get Messages for Minutes -> ERROR : 해당 날짜의 메시지 없음.  Start At : {}", aiGetMsgDTO.getStartTime());
            return null;
        }

        combinedMessages.sort(Comparator.comparing(ChatMessage::getSentAt));

        StringBuilder returnMessage = new StringBuilder();
        for (ChatMessage message : combinedMessages) {
            if (message.getSenderType() == SenderType.SYSTEM) continue;
            else if (message.getSenderType() == SenderType.AI) continue;

            returnMessage.append(message.getMessage()).append("/n");
        }

        if (!returnMessage.isEmpty()) returnMessage.setLength(returnMessage.length() - 1);
        log.debug("\nAI Get Messages for Minutes -> 합친 메시지 내역 확인\n");
        log.debug(returnMessage + "\n\n\n");

        return returnMessage.toString();
    }
}
