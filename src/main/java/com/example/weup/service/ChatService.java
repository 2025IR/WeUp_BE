package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.DisplayType;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.constant.SenderType;
import com.example.weup.dto.request.CreateChatRoomDTO;
import com.example.weup.dto.request.InviteChatRoomDTO;
import com.example.weup.dto.request.SendImageMessageRequestDTO;
import com.example.weup.dto.request.SendMessageRequestDTO;
import com.example.weup.dto.response.ChatPageResponseDto;
import com.example.weup.dto.response.GetChatRoomListDTO;
import com.example.weup.dto.response.ReceiveMessageResponseDto;
import com.example.weup.entity.*;
import com.example.weup.repository.*;
import com.example.weup.validate.ChatValidator;
import com.example.weup.validate.MemberValidator;
import com.example.weup.validate.ProjectValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService{

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatValidator chatValidator;
    private final ProjectValidator projectValidator;
    private final MemberValidator memberValidator;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional
    public void createChatRoom(User user, CreateChatRoomDTO createChatRoomDto) {

        Project project = projectValidator.validateActiveProject(createChatRoomDto.getProjectId());
        Member member = memberValidator.validateActiveMemberInProject(user.getUserId(), project.getProjectId());

        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomName(createChatRoomDto.getChatRoomName())
                .project(project)
                .basic(true)
                .build();

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .member(member)
                .chatRoom(chatRoom)
                .build();

        chatRoomRepository.save(chatRoom);
        chatRoomMemberRepository.save(chatRoomMember);
    }

    @Transactional
    public void inviteChatMember(Long chatRoomId, InviteChatRoomDTO inviteChatRoomDTO) throws JsonProcessingException {

        Project project = projectValidator.validateActiveProject(inviteChatRoomDTO.getProjectId());
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        for (Long memberId : inviteChatRoomDTO.getInviteMemberIds()) {
            Member member = memberValidator.validateMemberInChatRoom(project.getProjectId(), chatRoomId, memberId);

            ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                    .member(member)
                    .chatRoom(chatRoom)
                    .build();

            chatRoomMemberRepository.save(chatRoomMember);

            saveSystemMessage(chatRoomId, member.getUser().getName() + "님이 채팅방에 참여했습니다.");
        }
    }

    @Transactional
    public void leaveChatRoom(User user, Long chatRoomId) throws JsonProcessingException {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        Member member = memberValidator.validateActiveMemberInProject(user.getUserId(), chatRoom.getProject().getProjectId());

        memberValidator.isMemberAlreadyInChatRoom(chatRoom, member, true);

        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);

        chatRoomMemberRepository.delete(chatRoomMember);

        saveSystemMessage(chatRoomId, member.getUser().getName() + "님이 채팅방에서 퇴장했습니다.");

        List<ChatRoomMember> remainingMembers = chatRoomMemberRepository.findByChatRoom(chatRoom);

        if (remainingMembers.isEmpty()) {
            String key = "chat:room:" + chatRoomId;

            redisTemplate.delete(key);
            chatMessageRepository.deleteByChatRoom(chatRoom);
            chatRoomRepository.delete(chatRoom);

            log.info("chat room deleted -> roomId: {}", chatRoomId);
        }
    }


    @Transactional
    public void editChatRoomName(Long chatRoomId, String chatRoomName) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        chatRoom.editChatRoomName(chatRoomName);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public List<GetChatRoomListDTO> getChatRoomList(User user, Long projectId) {

        Project project = projectValidator.validateActiveProject(projectId);
        Member member = memberValidator.validateActiveMemberInProject(user.getUserId(), project.getProjectId());

        return chatRoomRepository.findByProject(project).stream()
                .map(chatRoom -> {
                    ChatRoomMember targetChatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);

                    List<String> chatRoomMemberNames = chatRoomMemberRepository.findByChatRoom(chatRoom).stream()
                            .map(chatRoomMember -> chatRoomMember.getMember().getUser().getName())
                            .collect(Collectors.toList());

                    return GetChatRoomListDTO.builder()
                            .chatRoomId(chatRoom.getChatRoomId())
                            .chatRoomMemberId(targetChatRoomMember.getChatRoomMemberId())
                            .chatRoomName(chatRoom.getChatRoomName())
                            .chatRoomMemberNames(chatRoomMemberNames)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ReceiveMessageResponseDto saveChatMessage(Long roomId, SendMessageRequestDTO dto) throws JsonProcessingException {
        String key = "chat:room:" + roomId;
        DisplayType displayType = DisplayType.DEFAULT;

        checkAndSendDateChangeMessage(roomId, dto.getSentAt());

        String lastJson = redisTemplate.opsForList().size(key) > 0
                ? redisTemplate.opsForList().index(key, -1)
                : null;

        if (lastJson != null) {
            SendMessageRequestDTO lastDto = objectMapper.readValue(lastJson, SendMessageRequestDTO.class);
            if (lastDto.getSenderId().equals(dto.getSenderId())) {
                if (lastDto.getSentAt().withSecond(0).withNano(0)
                        .equals(dto.getSentAt().withSecond(0).withNano(0))) {
                    displayType = DisplayType.SAME_TIME;
                } else {
                    displayType = DisplayType.SAME_SENDER;
                }
            }
        } else {
            ChatMessage lastMsg = chatMessageRepository.findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(roomId);
            if (lastMsg != null && lastMsg.getSenderId().getMemberId().equals(dto.getSenderId())) {
                if (lastMsg.getSentAt().withSecond(0).withNano(0)
                        .equals(dto.getSentAt().withSecond(0).withNano(0))) {
                    displayType = DisplayType.SAME_TIME;
                } else {
                    displayType = DisplayType.SAME_SENDER;
                }
            }
        }

        dto.setDisplayType(displayType);
        String jsonMessage = objectMapper.writeValueAsString(dto);

        redisTemplate.opsForList().rightPush(key, jsonMessage);
        log.info("websocket send chatting -> db read success : room id - {}, sender id - {}", roomId, dto.getSenderId());

        User sendUser = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        return ReceiveMessageResponseDto.builder()
                .senderId(dto.getSenderId())
                .senderName(sendUser.getName())
                .senderProfileImage(s3Service.getPresignedUrl(sendUser.getProfileImage()))
                .message(dto.getMessage())
                .sentAt(dto.getSentAt())
                .isImage(dto.getIsImage())
                .displayType(displayType)
                .build();
    }

    private void checkAndSendDateChangeMessage(Long roomId, LocalDateTime currentMessageTime) throws JsonProcessingException {
        String key = "chat:room:" + roomId;
        LocalDate currentDate = currentMessageTime.toLocalDate();

        LocalDate lastDate = null;

        Long redisSize = redisTemplate.opsForList().size(key);
        if (redisSize != null && redisSize > 0) {
            String lastJson = redisTemplate.opsForList().index(key, -1);
            if (lastJson != null) {
                SendMessageRequestDTO lastDto = objectMapper.readValue(lastJson, SendMessageRequestDTO.class);
                lastDate = lastDto.getSentAt().toLocalDate();
            }
        }

        if (lastDate == null) {
            ChatMessage lastMessage = chatMessageRepository.findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(roomId);
            if (lastMessage != null) {
                lastDate = lastMessage.getSentAt().toLocalDate();
            }
        }

        if (lastDate == null || !lastDate.equals(currentDate)) {
            saveSystemMessage(roomId, currentDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        }
    }

    @Transactional
    public void saveSystemMessage(Long roomId, String message) throws JsonProcessingException {
        SendMessageRequestDTO sendMessageRequestDTO = SendMessageRequestDTO.builder()
                .senderId(null)
                .senderType(SenderType.SYSTEM)
                .message(message)
                .isImage(false)
                .sentAt(LocalDateTime.now())
                .displayType(DisplayType.DEFAULT)
                .build();

        String jsonMessage = objectMapper.writeValueAsString(sendMessageRequestDTO);
        String key = "chat:room:" + roomId;
        redisTemplate.opsForList().rightPush(key, jsonMessage);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId,
                ReceiveMessageResponseDto.builder()
                        .senderId(null)
                        .senderName("System")
                        .senderProfileImage(null)
                        .message(message)
                        .isImage(false)
                        .sentAt(sendMessageRequestDTO.getSentAt())
                        .displayType(DisplayType.DEFAULT)
                        .build());
    }

    @Transactional
    public void handleImageMessage(SendImageMessageRequestDTO sendImageMessageRequestDTO) throws IOException {

        if (sendImageMessageRequestDTO.getFile() == null || sendImageMessageRequestDTO.getFile().isEmpty()) {
            throw new GeneralException(ErrorInfo.FILE_UPLOAD_FAILED);
        }

        String storedFileName = s3Service.uploadSingleFile(sendImageMessageRequestDTO.getFile()).getStoredFileName();

        SendMessageRequestDTO dto = SendMessageRequestDTO.builder()
                .senderId(Long.parseLong(sendImageMessageRequestDTO.getUserId()))
                .message(s3Service.getPresignedUrl(storedFileName))
                .isImage(true)
                .sentAt(LocalDateTime.now())
                .build();

        SendMessageRequestDTO saveDTO = SendMessageRequestDTO.builder()
                .senderId(Long.parseLong(sendImageMessageRequestDTO.getUserId()))
                .message(storedFileName)
                .isImage(true)
                .sentAt(LocalDateTime.now())
                .build();

        saveChatMessage(Long.parseLong(sendImageMessageRequestDTO.getRoomId()), saveDTO);

        messagingTemplate.convertAndSend("/topic/chat/" + sendImageMessageRequestDTO.getRoomId(), dto);
    }

    @Transactional
    @Scheduled(fixedDelay = 300000)
    public void flushAllRooms() throws JsonProcessingException {

        LocalDateTime now = LocalDateTime.now();
        log.info("flush all rooms chatting -> start, time : {}", now);

        List<Long> activeRoomIds = getAllActiveRoomIds();
        log.info("flush all rooms chatting -> db read success : data size : {}", activeRoomIds.size());

        for (Long roomId : activeRoomIds) {
            log.info("flush all rooms chatting -> db read success : room id : {}", roomId);
            String key = "chat:room:" + roomId;
            flushMessagesToDb(roomId, key);
        }
    }

    private List<Long> getAllActiveRoomIds() {

        Set<String> keys = redisTemplate.keys("chat:room:*");

        if (Objects.requireNonNull(keys).isEmpty()) {
            return Collections.emptyList();
        }

        return keys.stream()
                .map(key -> {
                    String[] parts = key.split(":");
                    return Long.parseLong(parts[2]);
                })
                .collect(Collectors.toList());
    }

    private void flushMessagesToDb(Long roomId, String key) throws JsonProcessingException {

        List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

        if (messages != null && !messages.isEmpty()) {

            List<ChatMessage> chatMessageList = new ArrayList<>();

            ChatRoom chatRoom = chatValidator.validateChatRoom(roomId);
            for (String json : messages) {
                SendMessageRequestDTO dto = objectMapper.readValue(json, SendMessageRequestDTO.class);

                User chatUser = userRepository.findById(dto.getSenderId())
                        .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

                ChatMessage chatMessage = ChatMessage.builder()
                        .chatRoom(chatRoom)
                        .user(chatUser)
                        .message(dto.getMessage())
                        .sentAt(dto.getSentAt())
                        .isImage(dto.getIsImage())
                        .displayType(dto.getDisplayType())
                        .build();

                chatMessageList.add(chatMessage);
            }

            chatMessageRepository.saveAll(chatMessageList);
            log.info("flush chat message to db -> success : data size - {}", chatMessageList.size());

            redisTemplate.delete(key);
            log.info("flush chat message to db -> success : delete redis key - {}", key);
        }
    }

    @Transactional(readOnly = true)
    public ChatPageResponseDto getChatMessages(Long roomId, int page, int size) throws JsonProcessingException {

        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoom_ChatRoomId(roomId);
        log.info("get chat message -> db read success : data size : {}", chatMessages.size());

        String key = "chat:room:" + roomId;
        List<String> redisMessages = redisTemplate.opsForList().range(key, 0, -1);

        List<ChatMessage> redisChatMessages = new ArrayList<>();

        if (redisMessages != null) {
            log.info("get redis chat message -> redis db read success : data size : {}", redisMessages.size());

            ChatRoom chatRoom = chatValidator.validateChatRoom(roomId);
            for (String json : redisMessages) {
                SendMessageRequestDTO dto = objectMapper.readValue(json, SendMessageRequestDTO.class);

                User chatUser = userRepository.findById(dto.getSenderId())
                        .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

                ChatMessage chatMessage = ChatMessage.builder()
                        .chatRoom(chatRoom)
                        .user(chatUser)
                        .message(dto.getMessage())
                        .sentAt(dto.getSentAt())
                        .isImage(dto.getIsImage())
                        .displayType(dto.getDisplayType())
                        .build();

                redisChatMessages.add(chatMessage);
            }
        }

        List<ChatMessage> combinedMessages = new ArrayList<>();
        combinedMessages.addAll(redisChatMessages);
        combinedMessages.addAll(chatMessages);
        combinedMessages.sort(Comparator.comparing(ChatMessage::getSentAt).reversed());

        int totalSize = combinedMessages.size();
        int start = page * size;
        int end = Math.min(start + size, totalSize);
        boolean isLastPage = end >= totalSize;

        List<ReceiveMessageResponseDto> messages = combinedMessages.subList(start, end).stream()
                .map(msg -> ReceiveMessageResponseDto.builder()
                        .senderId(msg.getUser().getUserId())
                        .senderName(msg.getUser().getName())
                        .senderProfileImage(s3Service.getPresignedUrl(msg.getUser().getProfileImage()))
                        .message(msg.getIsImage() ? s3Service.getPresignedUrl(msg.getMessage()) : msg.getMessage())
                        .isImage(msg.getIsImage())
                        .sentAt(msg.getSentAt())
                        .displayType(msg.getDisplayType())
                        .build())
                .toList();

        List<ReceiveMessageResponseDto> reverseMessages = new ArrayList<>(messages);
        Collections.reverse(reverseMessages);

        log.info("get chat message -> success : db size - {}", reverseMessages.size());
        return ChatPageResponseDto.builder()
                .messageList(reverseMessages)
                .page(page)
                .isLastPage(isLastPage)
                .build();
    }
}
