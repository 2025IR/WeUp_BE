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

    @Transactional
    public ChatMessage testPrepareSaveMsg(Long chatRoomId, SendMessageRequestDTO messageRequestDTO) throws JsonProcessingException {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

        Member sendMember = memberValidator.validateMember(chatRoom.getProject().getProjectId(), messageRequestDTO.getSenderId());
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, sendMember, true);

        checkAndSendDateChangeMessage(chatRoomId, messageRequestDTO.getSentAt());

        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .member(sendMember)
                .message(messageRequestDTO.getMessage())
                .isImage(messageRequestDTO.getIsImage())
                .sentAt(messageRequestDTO.getSentAt())
                .displayType(setDisplayType(chatRoomId, messageRequestDTO.getSenderId(), messageRequestDTO.getSentAt()))
                .build();
    }

    @Transactional
    public void testSaveMsg(Long chatRoomId, ChatMessage message) throws JsonProcessingException {

        String key = "chat:room:" + chatRoomId;
        redisTemplate.opsForZSet().add(key, objectMapper.writeValueAsString(message), message.getSentAt().toEpochSecond(ZoneOffset.UTC));

        ReceiveMessageResponseDto receiveMessageResponseDto = ReceiveMessageResponseDto.fromEntity(message);
        receiveMessageResponseDto.setSenderProfileImage(s3Service.getPresignedUrl(message.getMember().getUser().getProfileImage()));
        messagingTemplate.convertAndSend("/topic/chat" + chatRoomId, receiveMessageResponseDto);
    }

    // todo. system message
    @Transactional
    public ChatMessage testPrepareSaveSystemMsg(Long chatRoomId, String message) throws JsonProcessingException {

        return ChatMessage.builder()
                .member(null)
                .senderType(SenderType.SYSTEM)
                .message(message)
                .isImage(false)
                .sentAt(LocalDateTime.now())
                .displayType(DisplayType.DEFAULT)
                .build();
    }

    // todo. ai message

    // todo. image chat message


    @Transactional
    public ReceiveMessageResponseDto saveChatMessage(Long chatRoomId, SendMessageRequestDTO dto) throws JsonProcessingException {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));
        Member sendMember = memberValidator.validateMember(chatRoom.getProject().getProjectId(), dto.getSenderId());
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, sendMember, true);

        checkAndSendDateChangeMessage(chatRoomId, dto.getSentAt());

        DisplayType displayType = setDisplayType(chatRoomId, dto.getSenderId(), SenderType.MEMBER, dto.getSentAt());

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .member(sendMember)
                .message(dto.getMessage())
                .sentAt(dto.getSentAt())
                .isImage(dto.getIsImage())
                .displayType(displayType)
                .build();

        String key = "chat:room:" + chatRoomId;
        String jsonMessage = objectMapper.writeValueAsString(message);
        redisTemplate.opsForList().rightPush(key, jsonMessage);
        log.info("websocket send chatting -> db read success : room id - {}, sender id - {}", chatRoomId, dto.getSenderId());

        return ReceiveMessageResponseDto.builder()
                .senderId(dto.getSenderId())
                .senderName(sendMember.getUser().getName())
                .senderProfileImage(s3Service.getPresignedUrl(sendMember.getUser().getProfileImage()))
                .message(dto.getMessage())
                .sentAt(dto.getSentAt())
                .senderType(SenderType.MEMBER)
                .isImage(dto.getIsImage())
                .displayType(displayType)
                .build();
    }

    private DisplayType setDisplayType(Long chatRoomId, Long senderId, SenderType senderType, LocalDateTime sentAt) throws JsonProcessingException {
        String key = "chat:room:" + chatRoomId;

        String lastJson = redisTemplate.opsForList().size(key) > 0
                ? redisTemplate.opsForList().index(key, -1)
                : null;

        ChatMessage lastMessage;

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

        String key = "chat:room:" + chatRoomId;
        Long redisSize = redisTemplate.opsForList().size(key);

        LocalDate currentDate = currentMessageTime.toLocalDate();
        LocalDate lastDate = null;

        if (redisSize != null && redisSize > 0) {
            String lastJson = redisTemplate.opsForList().index(key, -1);
            SendMessageRequestDTO lastDto = objectMapper.readValue(lastJson, SendMessageRequestDTO.class);
            lastDate = lastDto.getSentAt().toLocalDate();
        }
        if(lastDate == null) {
            ChatMessage lastMessage = chatMessageRepository.findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(chatRoomId);
            if (lastMessage != null) {
                lastDate = lastMessage.getSentAt().toLocalDate();
            }
        }

        if (lastDate == null || !lastDate.equals(currentDate)) {
            saveSystemMessage(chatRoomId, currentDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        }
    }

    @Transactional
    public void saveSystemMessage(Long roomId, String message) throws JsonProcessingException {
        ChatMessage chatMessage = ChatMessage.builder()
                .member(null)
                .senderType(SenderType.SYSTEM)
                .message(message)
                .isImage(false)
                .sentAt(LocalDateTime.now())
                .displayType(DisplayType.DEFAULT)
                .build();

        String jsonMessage = objectMapper.writeValueAsString(chatMessage);
        String key = "chat:room:" + roomId;
        redisTemplate.opsForList().rightPush(key, jsonMessage);

        messagingTemplate.convertAndSend("/topic/chat/" + roomId,
                ReceiveMessageResponseDto.builder()
                        .senderId(null)
                        .senderName("System")
                        .senderProfileImage(null)
                        .message(message)
                        .isImage(false)
                        .sentAt(chatMessage.getSentAt())
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
