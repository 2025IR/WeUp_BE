package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.DisplayType;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.constant.SenderType;
import com.example.weup.dto.request.SendImageMessageRequestDTO;
import com.example.weup.dto.request.SendMessageRequestDTO;
import com.example.weup.dto.response.ChatPageResponseDto;
import com.example.weup.dto.response.ReceiveMessageResponseDto;
import com.example.weup.entity.*;
import com.example.weup.repository.*;
import com.example.weup.validate.ChatValidator;
import com.example.weup.validate.MemberValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService{

    private final MemberRepository memberRepository;

    private final ChatRoomRepository chatRoomRepository;

    private final ChatMessageRepository chatMessageRepository;

    private final S3Service s3Service;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    private final ChatValidator chatValidator;

    private final MemberValidator memberValidator;

    // basic chat message
    @Transactional
    public void testSendBasicMsg(Long chatRoomId, SendMessageRequestDTO messageRequestDTO) throws JsonProcessingException {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        Member sendMember = memberValidator.validateMember(chatRoom.getProject().getProjectId(), messageRequestDTO.getSenderId());
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, sendMember, true);

        ChatMessage basicMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .member(sendMember)
                .message(messageRequestDTO.getMessage())
                .isImage(messageRequestDTO.getIsImage())
                .sentAt(messageRequestDTO.getSentAt())
                .displayType(setDisplayType(chatRoomId, messageRequestDTO.getSenderId(), SenderType.MEMBER, messageRequestDTO.getSentAt()))
                .build();

        checkAndSendDateChangeMessage(chatRoomId, basicMessage.getSentAt());
        testSaveMsg(chatRoomId, basicMessage);
    }

    // image chat message
    @Transactional
    public void testSendImgMsg(Long chatRoomId, SendImageMessageRequestDTO sendImageMessageRequestDTO) throws IOException {

        if (sendImageMessageRequestDTO.getFile() == null || sendImageMessageRequestDTO.getFile().isEmpty()) {
            throw new GeneralException(ErrorInfo.FILE_UPLOAD_FAILED);
        }

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        Member sendMember = memberValidator.validateMember(chatRoom.getProject().getProjectId(), sendImageMessageRequestDTO.getSenderId());
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, sendMember, true);

        String storedFileName = s3Service.uploadSingleFile(sendImageMessageRequestDTO.getFile()).getStoredFileName();

        ChatMessage imgMessage = ChatMessage.builder()
                .member(sendMember)
                .senderType(SenderType.MEMBER)
                .message(s3Service.getPresignedUrl(storedFileName))
                .isImage(true)
                .sentAt(LocalDateTime.now())
                .displayType(setDisplayType(chatRoomId, sendMember.getMemberId(), SenderType.MEMBER, LocalDateTime.now()))
                .build();

        checkAndSendDateChangeMessage(chatRoomId, imgMessage.getSentAt());
        testSaveMsg(chatRoomId, imgMessage);
    }

    // system message
    @Transactional
    public void testSendSystemMsg(Long chatRoomId, String message) throws JsonProcessingException {

        log.info("Send System Message 부분으로 넘어옴");
        ChatMessage systemMessage = ChatMessage.builder()
                .member(null)
                .senderType(SenderType.SYSTEM)
                .message(message)
                .isImage(false)
                .sentAt(LocalDateTime.now())
                .displayType(DisplayType.DEFAULT)
                .build();

        checkAndSendDateChangeMessage(chatRoomId, systemMessage.getSentAt());
        testSaveMsg(chatRoomId, systemMessage);
    }

    // ai message
    @Transactional
    public void testSendAIMsg(Long chatRoomId, String message) throws JsonProcessingException {

        ChatMessage aiMessage = ChatMessage.builder()
                .member(null)
                .senderType(SenderType.AI)
                .message(message)
                .isImage(false)
                .sentAt(LocalDateTime.now())
                .displayType(setDisplayType(chatRoomId, null, SenderType.AI, LocalDateTime.now()))
                .build();

        checkAndSendDateChangeMessage(chatRoomId, aiMessage.getSentAt());
        testSaveMsg(chatRoomId, aiMessage);
    }

    // send message
    private void testSaveMsg(Long chatRoomId, ChatMessage message) throws JsonProcessingException {

        log.info("Send Save Message 부분으로 넘어옴");
        String key = "chat:room:" + chatRoomId;
        redisTemplate.opsForZSet().add(key, objectMapper.writeValueAsString(message), message.getSentAt().toEpochSecond(ZoneOffset.UTC));

        ReceiveMessageResponseDto receiveMessageResponseDto = ReceiveMessageResponseDto.fromEntity(message);
        receiveMessageResponseDto.setSenderProfileImage(
                message.getSenderType() == SenderType.MEMBER
                        ? s3Service.getPresignedUrl(message.getMember().getUser().getProfileImage())
                        : null
        );

        messagingTemplate.convertAndSend("/topic/chat" + chatRoomId, receiveMessageResponseDto);
    }

    private DisplayType setDisplayType(Long chatRoomId, Long senderId, SenderType senderType, LocalDateTime sentAt) throws JsonProcessingException {

        ChatMessage lastMessage;
        String key = "chat:room:" + chatRoomId;

        String lastJson = redisTemplate.opsForZSet().size(key) > 0
                ? Objects.requireNonNull(redisTemplate.opsForZSet().range(key, -1, -1)).toString()
                : null;

        if (lastJson != null) {
            lastMessage = objectMapper.readValue(lastJson, ChatMessage.class);
        } else {
            lastMessage = chatMessageRepository.findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(chatRoomId);
        }

        if (lastMessage != null) {
            if (lastMessage.getMember() != null) {
                if (lastMessage.getMember().getMemberId().equals(senderId)) {
                    if (isSameMinute(lastMessage.getSentAt(), sentAt)) {
                        return DisplayType.SAME_TIME;
                    } else {
                        return DisplayType.SAME_SENDER;
                    }
                }
            } else if (lastMessage.getSenderType() != null) {
                if (lastMessage.getSenderType().equals(senderType)) {
                    if (isSameMinute(lastMessage.getSentAt(), sentAt)) {
                        return DisplayType.SAME_TIME;
                    } else {
                        return DisplayType.SAME_SENDER;
                    }
                }
            }
        }

        return DisplayType.DEFAULT;
    }

    private boolean isSameMinute(LocalDateTime time1, LocalDateTime time2) {
        if (time1 == null || time2 == null) return false;
        return time1.withSecond(0).withNano(0).equals(time2.withSecond(0).withNano(0));
    }

    private void checkAndSendDateChangeMessage(Long chatRoomId, LocalDateTime currentMessageTime) throws JsonProcessingException {

        log.info("Check And Send Data Change Message 부분으로 넘어옴");
        String key = "chat:room:" + chatRoomId;
        Long redisSize = redisTemplate.opsForZSet().size(key);

        LocalDate currentDate = currentMessageTime.toLocalDate();
        LocalDate lastMessageDate = null;

        Set<String> lastMessages = redisTemplate.opsForZSet().range(key, -1, -1);
        if (lastMessages != null && !lastMessages.isEmpty()) {
            log.info("첫 번째 IF문 - Redis에 무언가 있을 때");

            String lastMessage = lastMessages.iterator().next();
            ChatMessage lastChatMessage = objectMapper.readValue(lastMessage, ChatMessage.class);
            lastMessageDate = lastChatMessage.getSentAt().toLocalDate();
        }

        if (lastMessageDate == null) {
            log.info("두 번째 IF문 - Redis에 없어서 MySQL에서 가져올 때");
            ChatMessage lastMessage = chatMessageRepository.findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(chatRoomId);
            if (lastMessage != null) {
                log.info("세 번째 IF문 - MySQL에서 가져왔고, 그게 Null이 아닐 떄");
                lastMessageDate = lastMessage.getSentAt().toLocalDate();
            }
        }

        if (lastMessageDate == null || !lastMessageDate.equals(currentDate)) {
            log.info("네 번째 IF문 - 이전 채팅 내역이 아예 없을 때");

            ChatMessage systemMessage = ChatMessage.builder()
                    .member(null)
                    .senderType(SenderType.SYSTEM)
                    .message(currentDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")))
                    .isImage(false)
                    .sentAt(LocalDateTime.now())
                    .displayType(DisplayType.DEFAULT)
                    .build();

            testSaveMsg(chatRoomId, systemMessage);
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 300000)
    public void flushAllRooms() throws JsonProcessingException {

        LocalDateTime now = LocalDateTime.now();
        log.info("flush all rooms chatting -> start, time : {}", now);

        Set<Long> activeRoomIds = getAllActiveRoomIds();
        log.info("flush all rooms chatting -> db read success : data size : {}", activeRoomIds.size());

        for (Long roomId : activeRoomIds) {
            log.info("flush all rooms chatting -> db read success : room id : {}", roomId);
            String key = "chat:room:" + roomId;
            flushMessagesToDb(key);
        }
    }

    private Set<Long> getAllActiveRoomIds() {

        Set<String> keys = redisTemplate.keys("chat:room:*");
        if (Objects.requireNonNull(keys).isEmpty()) {
            return new HashSet<>();
        }

        return keys.stream()
                .map(key -> {
                    String[] parts = key.split(":");
                    return Long.parseLong(parts[2]);
                })
                .collect(Collectors.toSet());
    }

    // flush message to db
    private void flushMessagesToDb(String key) throws JsonProcessingException {

        Set<String> messages = redisTemplate.opsForZSet().range(key, 0, -1);
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<ChatMessage> chatMessages = new ArrayList<>();
        for (String message : messages) {
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
            chatMessages.add(chatMessage);
        }

        chatMessageRepository.saveAll(chatMessages);
        log.info("flush chat message to db -> success : data size - {}", chatMessages.size());

        redisTemplate.delete(key);
        log.info("flush chat message to db -> success : delete redis key - {}", key);
    }

    @Transactional(readOnly = true)
    public ChatPageResponseDto getChatMessages(Long chatRoomId, int page, int size) throws JsonProcessingException {

        String key = "chat:room:" + chatRoomId;
        List<String> redisMessages = redisTemplate.opsForList().range(key, 0, -1);

        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoom_ChatRoomId(chatRoomId);
        log.info("get chat message -> db read success : data size : {}", chatMessages.size());

        List<ChatMessage> redisChatMessages = new ArrayList<>();

        if (redisMessages != null) {
            log.info("get redis chat message -> redis db read success : data size : {}", redisMessages.size());

            ChatRoom chatRoom = chatValidator.validateChatRoom(chatRoomId);

            for (String json : redisMessages) {
                ChatMessage redisMessage = objectMapper.readValue(json, ChatMessage.class);

                Member chatMember = redisMessage.getMember() != null
                        ? memberRepository.findById(redisMessage.getMember().getMemberId())
                        .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND))
                        : null;

                ChatMessage chatMessage = ChatMessage.builder()
                        .chatRoom(chatRoom)
                        .member(chatMember)
                        .message(redisMessage.getMessage())
                        .sentAt(redisMessage.getSentAt())
                        .isImage(redisMessage.getIsImage())
                        .senderType(redisMessage.getSenderType())
                        .displayType(redisMessage.getDisplayType())
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
                        .senderId(msg.getMember() != null ? msg.getMember().getMemberId() : null)
                        .senderName(msg.getMember() != null ? msg.getMember().getUser().getName() : null)
                        .senderProfileImage(msg.getMember() != null
                                ? s3Service.getPresignedUrl(msg.getMember().getUser().getProfileImage())
                                : null)
                        .message(msg.getIsImage() ? s3Service.getPresignedUrl(msg.getMessage()) : msg.getMessage())
                        .isImage(msg.getIsImage())
                        .sentAt(msg.getSentAt())
                        .senderType(msg.getSenderType())
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
