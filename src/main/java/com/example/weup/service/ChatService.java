package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.ChatMessageRequestDto;
import com.example.weup.dto.response.ChatMessageResponseDto;
import com.example.weup.entity.ChatMessage;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Member;
import com.example.weup.entity.User;
import com.example.weup.repository.ChatMessageRepository;
import com.example.weup.repository.ChatRoomRepository;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService{

    private final ChatMessageRepository chatMessageRepository;

    private final ChatRoomRepository chatRoomRepository;

    private final UserRepository userRepository;

    private final MemberRepository memberRepository;

    private final S3Service s3Service;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void saveChatMessage(Long roomId, ChatMessageRequestDto dto) throws JsonProcessingException {

        String key = "chat:room:" + roomId;
        String jsonMessage = objectMapper.writeValueAsString(dto);

        redisTemplate.opsForList().rightPush(key, jsonMessage);
    }

    @Transactional
    public void handleImageMessage(Long projectId, Long roomId, Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(ErrorInfo.FILE_UPLOAD_FAILED);
        }

        // 1. senderId(memberId) 조회
        Optional<Member> memberOpt = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId);

        Long memberId = memberOpt
                .map(Member::getMemberId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        // 2. S3 업로드
        String storedFileName = s3Service.uploadSingleFile(file).getStoredFileName();

        // 3. DTO 구성
        ChatMessageRequestDto dto = ChatMessageRequestDto.builder()
                .projectId(projectId)
                .senderId(memberId)
                .message(s3Service.getPresignedUrl(storedFileName))
                .isImage(true)
                .sentAt(LocalDateTime.now())
                .build();


        saveChatMessage(roomId, dto);

        // 4. WebSocket 전송
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, dto);
    }

    @Transactional
    @Scheduled(fixedDelay = 300000)
    public void flushAllRooms() throws JsonProcessingException {

        List<Long> activeRoomIds = getAllActiveRoomIds();

        for (Long roomId : activeRoomIds) {
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
                    return Long.parseLong(parts[2]);  // roomId만 추출
                })
                .collect(Collectors.toList());
    }

    private void flushMessagesToDb(Long roomId, String key) throws JsonProcessingException {

        List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

        if (messages != null && !messages.isEmpty()) {

            List<ChatMessage> chatMessageList = new ArrayList<>();

            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

            for (String json : messages) {
                ChatMessageRequestDto dto = objectMapper.readValue(json, ChatMessageRequestDto.class);

                User chatUser = userRepository.findById(dto.getSenderId())
                        .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

                ChatMessage chatMessage = ChatMessage.builder()
                        .chatRoom(chatRoom)
                        .user(chatUser)
                        .message(dto.getMessage())
                        .sentAt(dto.getSentAt())
                        .build();

                chatMessageList.add(chatMessage);
            }

            chatMessageRepository.saveAll(chatMessageList);

            redisTemplate.delete(key);
        }
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponseDto> getChatMessages(Long roomId, int page, int size) throws JsonProcessingException {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<ChatMessage> chatMessages = chatMessageRepository.findByChatRoom_ChatRoomId(roomId, pageable);

        String key = "chat:room:" + roomId;
        List<String> redisMessages = redisTemplate.opsForList().range(key, 0, -1);

        List<ChatMessage> redisChatMessages = new ArrayList<>();

        if (redisMessages != null) {

            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

            for (String json : redisMessages) {
                ChatMessageRequestDto dto = objectMapper.readValue(json, ChatMessageRequestDto.class);

                User chatUser = userRepository.findById(dto.getSenderId())
                        .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

                ChatMessage chatMessage = ChatMessage.builder()
                        .chatRoom(chatRoom)
                        .user(chatUser)
                        .message(dto.getMessage())
                        .sentAt(dto.getSentAt())
                        .build();

                redisChatMessages.add(chatMessage);
            }
        }

        List<ChatMessageResponseDto> combinedMessages = new ArrayList<>();

        redisChatMessages.sort(Comparator.comparing(ChatMessage::getSentAt).reversed());

        combinedMessages.addAll(redisChatMessages.stream().map(ChatMessageResponseDto::fromEntity).toList());
        combinedMessages.addAll(chatMessages.getContent().stream().map(ChatMessageResponseDto::fromEntity).toList());

        return new PageImpl<>(combinedMessages, pageable, chatMessages.getTotalElements() + redisChatMessages.size());
    }
}
