package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.SendImageMessageRequestDTO;
import com.example.weup.dto.request.SendMessageRequestDto;
import com.example.weup.dto.response.ChatPageResponseDto;
import com.example.weup.dto.response.ReceiveMessageResponseDto;
import com.example.weup.entity.ChatMessage;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Member;
import com.example.weup.entity.User;
import com.example.weup.repository.ChatMessageRepository;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.UserRepository;
import com.example.weup.validate.ChatValidator;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService{

    private final ChatMessageRepository chatMessageRepository;

    private final UserRepository userRepository;

    private final MemberRepository memberRepository;

    private final S3Service s3Service;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    private final ChatValidator chatValidator;

    @Transactional
    public ReceiveMessageResponseDto saveChatMessage(Long roomId, SendMessageRequestDto dto) throws JsonProcessingException {

        String key = "chat:room:" + roomId;
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
                .build();
    }

    @Transactional
    public void handleImageMessage(SendImageMessageRequestDTO sendImageMessageRequestDTO) throws IOException {

        if (sendImageMessageRequestDTO.getFile() == null || sendImageMessageRequestDTO.getFile().isEmpty()) {
            throw new GeneralException(ErrorInfo.FILE_UPLOAD_FAILED);
        }

        String storedFileName = s3Service.uploadSingleFile(sendImageMessageRequestDTO.getFile()).getStoredFileName();

        SendMessageRequestDto dto = SendMessageRequestDto.builder()
                .senderId(Long.parseLong(sendImageMessageRequestDTO.getUserId()))
                .message(s3Service.getPresignedUrl(storedFileName))
                .isImage(true)
                .sentAt(LocalDateTime.now())
                .build();

        SendMessageRequestDto saveDTO = SendMessageRequestDto.builder()
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
                SendMessageRequestDto dto = objectMapper.readValue(json, SendMessageRequestDto.class);

                User chatUser = userRepository.findById(dto.getSenderId())
                        .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

                ChatMessage chatMessage = ChatMessage.builder()
                        .chatRoom(chatRoom)
                        .user(chatUser)
                        .message(dto.getMessage())
                        .sentAt(dto.getSentAt())
                        .isImage(dto.getIsImage())
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
                SendMessageRequestDto dto = objectMapper.readValue(json, SendMessageRequestDto.class);

                User chatUser = userRepository.findById(dto.getSenderId())
                        .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

                ChatMessage chatMessage = ChatMessage.builder()
                        .chatRoom(chatRoom)
                        .user(chatUser)
                        .message(dto.getMessage())
                        .sentAt(dto.getSentAt())
                        .isImage(dto.getIsImage())
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
