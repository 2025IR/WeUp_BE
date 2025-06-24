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
import com.example.weup.repository.ChatRoomRepository;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.UserRepository;
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

    private final ChatRoomRepository chatRoomRepository;

    private final UserRepository userRepository;

    private final MemberRepository memberRepository;

    private final S3Service s3Service;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ReceiveMessageResponseDto saveChatMessage(Long roomId, SendMessageRequestDto dto) throws JsonProcessingException {

        String key = "chat:room:" + roomId;
        String jsonMessage = objectMapper.writeValueAsString(dto);

        redisTemplate.opsForList().rightPush(key, jsonMessage);
        log.debug("send message service 진입");
        log.debug("redis 저장 여부 확인" + redisTemplate.opsForList().index(key, -1));

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

        log.debug("handle image message service 진입");
        log.debug(String.valueOf(sendImageMessageRequestDTO.getProjectId()));
        log.debug(String.valueOf(sendImageMessageRequestDTO.getRoomId()));
        log.debug(String.valueOf(sendImageMessageRequestDTO.getUserId()));

        if (sendImageMessageRequestDTO.getFile() == null || sendImageMessageRequestDTO.getFile().isEmpty()) {
            throw new GeneralException(ErrorInfo.FILE_UPLOAD_FAILED);
        }

        Optional<Member> memberOpt = memberRepository.findByUser_UserIdAndProject_ProjectId(Long.parseLong(sendImageMessageRequestDTO.getUserId()), Long.parseLong(sendImageMessageRequestDTO.getProjectId()));

        if (memberOpt.isEmpty()) {
            log.error("Member 조회 실패: userId={}, projectId={}",
                    sendImageMessageRequestDTO.getUserId(),
                    sendImageMessageRequestDTO.getProjectId()
            );
            throw new GeneralException(ErrorInfo.TODO_NOT_FOUND);
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

        log.debug("flush all rooms 실행 시간");
        LocalDateTime now = LocalDateTime.now();
        log.debug(String.valueOf(now));

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

            redisTemplate.delete(key);
        }
    }

    @Transactional(readOnly = true)
    public ChatPageResponseDto getChatMessages(Long roomId, int page, int size) throws JsonProcessingException {

        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoom_ChatRoomId(roomId);
        log.debug("get mysql chat messages 개수 확인");
        log.debug(String.valueOf(chatMessages.size()));

        String key = "chat:room:" + roomId;
        List<String> redisMessages = redisTemplate.opsForList().range(key, 0, -1);

        List<ChatMessage> redisChatMessages = new ArrayList<>();

        if (redisMessages != null) {
            log.debug("get redis chat messages 개수 확인");
            log.debug(String.valueOf(redisMessages.size()));

            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.CHAT_ROOM_NOT_FOUND));

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

        log.debug("계산 값 확인하기");
        log.debug("totalSize: " + totalSize + ", start: " + start + ", end: " + end + ", isLastPage: " + isLastPage);

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

        log.debug("첫 번째 값 확인");
        log.debug(messages.getFirst().getMessage());
        log.debug(messages.getFirst().getSenderName());
        log.debug(String.valueOf(messages.getFirst().getSentAt()));
        log.debug(String.valueOf(messages.getFirst().getSenderId()));

        log.debug("마지막 값 확인");
        log.debug(messages.getLast().getMessage());
        log.debug(messages.getLast().getSenderName());
        log.debug(String.valueOf(messages.getLast().getSentAt()));
        log.debug(String.valueOf(messages.getLast().getSenderId()));

        return ChatPageResponseDto.builder()
                .messageList(reverseMessages)
                .page(page)
                .isLastPage(isLastPage)
                .build();
    }
}
