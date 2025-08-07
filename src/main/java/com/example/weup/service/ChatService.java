package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.DisplayType;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.constant.SenderType;
import com.example.weup.dto.request.GetPageable;
import com.example.weup.dto.request.SendImageMessageRequestDTO;
import com.example.weup.dto.request.SendMessageRequestDTO;
import com.example.weup.dto.response.ReceiveMessageResponseDTO;
import com.example.weup.dto.response.RedisMessageDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.*;
import com.example.weup.validate.ChatValidator;
import com.example.weup.validate.MemberValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
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

    private final ChatMessageRepository chatMessageRepository;

    private final S3Service s3Service;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    private final ChatValidator chatValidator;

    private final MemberValidator memberValidator;

    // basic chat message
    @Transactional
    public void sendBasicMessage(Long chatRoomId, SendMessageRequestDTO messageRequestDTO) throws JsonProcessingException {

        ChatRoom chatRoom = chatValidator.validateChatRoom(chatRoomId);

        Member sendMember = memberValidator.validateMemberAndProject(messageRequestDTO.getSenderId());
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, sendMember, true);

        checkAndSendDateChangeMessage(chatRoomId, LocalDateTime.now());

        RedisMessageDTO basicMessage = RedisMessageDTO.builder()
                .chatRoomId(chatRoomId)
                .memberId(sendMember.getMemberId())
                .message(messageRequestDTO.getMessage())
                .isImage(messageRequestDTO.getIsImage())
                .sentAt(messageRequestDTO.getSentAt())
                .displayType(setDisplayType(chatRoomId, messageRequestDTO.getSenderId(), SenderType.MEMBER, messageRequestDTO.getSentAt()))
                .build();

        saveMessage(chatRoomId, basicMessage);
    }

    // image chat message
    @Transactional
    public void sendImageMessage(Long chatRoomId, SendImageMessageRequestDTO sendImageMessageRequestDTO) throws IOException {

        if (sendImageMessageRequestDTO.getFile() == null || sendImageMessageRequestDTO.getFile().isEmpty()) {
            throw new GeneralException(ErrorInfo.FILE_UPLOAD_FAILED);
        }

        ChatRoom chatRoom = chatValidator.validateChatRoom(chatRoomId);

        Member sendMember = memberValidator.validateMemberAndProject(sendImageMessageRequestDTO.getSenderId());
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, sendMember, true);

        checkAndSendDateChangeMessage(chatRoomId, LocalDateTime.now());
        String storedFileName = s3Service.uploadSingleFile(sendImageMessageRequestDTO.getFile()).getStoredFileName();

        RedisMessageDTO imgMessage = RedisMessageDTO.builder()
                .chatRoomId(chatRoomId)
                .memberId(sendMember.getMemberId())
                .message(storedFileName)
                .isImage(true)
                .sentAt(LocalDateTime.now())
                .displayType(setDisplayType(chatRoomId, sendMember.getMemberId(), SenderType.MEMBER, LocalDateTime.now()))
                .build();

        saveMessage(chatRoomId, imgMessage);
    }

    // system message
    @Transactional
    public void sendSystemMessage(Long chatRoomId, String message) throws JsonProcessingException {

        log.info("Send System Message 부분으로 넘어옴");
        chatValidator.validateChatRoom(chatRoomId);

        checkAndSendDateChangeMessage(chatRoomId, LocalDateTime.now());

        RedisMessageDTO systemMessage = RedisMessageDTO.builder()
                .chatRoomId(chatRoomId)
                .memberId(null)
                .message(message)
                .isImage(false)
                .sentAt(LocalDateTime.now())
                .senderType(SenderType.SYSTEM)
                .displayType(DisplayType.DEFAULT)
                .build();

        saveMessage(chatRoomId, systemMessage);
    }

    // ai message
    @Transactional
    public void sendAIMessage(Long chatRoomId, String message, String originalUserMessage, String originalSenderName) throws JsonProcessingException {

        chatValidator.validateChatRoom(chatRoomId);

        checkAndSendDateChangeMessage(chatRoomId, LocalDateTime.now());

        RedisMessageDTO aiMessage = RedisMessageDTO.builder()
                .chatRoomId(chatRoomId)
                .memberId(null)
                .message(message)
                .isImage(false)
                .sentAt(LocalDateTime.now())
                .senderType(SenderType.AI)
                .displayType(setDisplayType(chatRoomId, null, SenderType.AI, LocalDateTime.now()))
                .originalMessage(originalUserMessage)
                .originalSenderName(originalSenderName)
                .build();

        saveMessage(chatRoomId, aiMessage);
    }

    // send message
    private void saveMessage(Long chatRoomId, RedisMessageDTO message) throws JsonProcessingException {

        log.info("Send Save Message 부분으로 넘어옴");
        String key = "chat:room:" + chatRoomId;
        redisTemplate.opsForZSet().add(key, objectMapper.writeValueAsString(message), message.getSentAt().toEpochSecond(ZoneOffset.UTC));

        ReceiveMessageResponseDTO receiveMessageResponseDto = ReceiveMessageResponseDTO.fromRedisMessageDTO(message);
        setReceiveMessageField(receiveMessageResponseDto);

        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, receiveMessageResponseDto);
    }

    private DisplayType setDisplayType(Long chatRoomId, Long senderId, SenderType senderType, LocalDateTime sentAt) throws JsonProcessingException {

        ChatMessage lastMessage;
        String key = "chat:room:" + chatRoomId;

        Set<String> lastSet = redisTemplate.opsForZSet().range(key, -1, -1);
        String lastJson = (lastSet != null && !lastSet.isEmpty())
                ? lastSet.iterator().next()
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

        LocalDate currentDate = currentMessageTime.toLocalDate();
        LocalDate lastMessageDate = null;

        Set<String> lastMessages = redisTemplate.opsForZSet().range(key, -1, -1);
        if (lastMessages != null && !lastMessages.isEmpty()) {

            String lastMessage = lastMessages.iterator().next();
            ChatMessage lastChatMessage = objectMapper.readValue(lastMessage, ChatMessage.class);
            lastMessageDate = lastChatMessage.getSentAt().toLocalDate();
        }

        if (lastMessageDate == null) {
            ChatMessage lastMessage = chatMessageRepository.findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(chatRoomId);
            if (lastMessage != null) {
                lastMessageDate = lastMessage.getSentAt().toLocalDate();
            }
        }

        if (lastMessageDate == null || !lastMessageDate.equals(currentDate)) {
            RedisMessageDTO systemMessage = RedisMessageDTO.builder()
                    .chatRoomId(chatRoomId)
                    .memberId(null)
                    .message(currentDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")))
                    .isImage(false)
                    .sentAt(currentMessageTime)
                    .senderType(SenderType.SYSTEM)
                    .displayType(DisplayType.DEFAULT)
                    .build();

            saveMessage(chatRoomId, systemMessage);
        }
    }

    private ReceiveMessageResponseDTO setReceiveMessageField(ReceiveMessageResponseDTO messageDTO) {

        if (messageDTO.getSenderType() == SenderType.MEMBER) {
            Member member = memberRepository.findById(messageDTO.getSenderId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

            messageDTO.setSenderName(member.getUser().getName());
            messageDTO.setSenderProfileImage(s3Service.getPresignedUrl(member.getUser().getProfileImage()));

            if (messageDTO.isImage()) messageDTO.setMessage(s3Service.getPresignedUrl(messageDTO.getMessage()));

        } else if (messageDTO.getSenderType() == SenderType.AI) {
            messageDTO.setSenderId(null);
            messageDTO.setSenderName(SenderType.AI.getName());
            messageDTO.setSenderProfileImage(s3Service.getPresignedUrl(SenderType.AI.getProfileImage()));

        } else if (messageDTO.getSenderType() == SenderType.SYSTEM) {
            messageDTO.setSenderId(null);
            messageDTO.setSenderName(SenderType.SYSTEM.getName());

        } else if (messageDTO.getSenderType() == SenderType.WITHDRAW) {
            messageDTO.setSenderId(null);
            messageDTO.setSenderName(SenderType.WITHDRAW.getName());
            messageDTO.setSenderProfileImage(s3Service.getPresignedUrl(SenderType.WITHDRAW.getProfileImage()));

        } else throw new GeneralException(ErrorInfo.INTERNAL_ERROR);

        return messageDTO;
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
            RedisMessageDTO redisMessage = objectMapper.readValue(message, RedisMessageDTO.class);

            ChatMessage chatMessage = translateRedisDtoIntoChatMessage(redisMessage);
            chatMessages.add(chatMessage);
        }

        chatMessageRepository.saveAll(chatMessages);
        log.info("flush chat message to db -> success : data size - {}", chatMessages.size());

        redisTemplate.delete(key);
        log.info("flush chat message to db -> success : delete redis key - {}", key);
    }

    private ChatMessage translateRedisDtoIntoChatMessage(RedisMessageDTO message) {

        ChatRoom chatRoom = chatValidator.validateChatRoom(message.getChatRoomId());

        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .member(message.getMemberId() != null
                        ? memberRepository.findById(message.getMemberId()).orElseThrow(
                                () -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND))
                        : null)
                .message(message.getMessage())
                .isImage(message.getIsImage())
                .sentAt(message.getSentAt())
                .senderType(message.getSenderType())
                .displayType(message.getDisplayType())
                .originalMessage(message.getOriginalMessage())
                .originalSenderName(message.getOriginalSenderName())
                .build();
    }

    @Transactional
    public Slice<ReceiveMessageResponseDTO> getChatMessages(Long chatRoomId, GetPageable pageable) throws JsonProcessingException {

        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        chatValidator.validateChatRoom(chatRoomId);

        String redisKey = "chat:room:" + chatRoomId;
        int offset = pageable.getPage() * pageable.getSize();
        int fetchLimit = pageable.getSize() + 1;

        Long redisCount = zSetOperations.size(redisKey);
        if (redisCount == null) redisCount = 0L;

        List<ChatMessage> combinedMessages = new ArrayList<>();

        if (offset + fetchLimit <= redisCount) {
            Set<String> redisMessagesJson = zSetOperations.reverseRange(redisKey, offset, fetchLimit); // 0~20 (21개 가져옴)

            if (redisMessagesJson != null && !redisMessagesJson.isEmpty()) {
                for (String json : redisMessagesJson) {
                    combinedMessages.add(translateRedisDtoIntoChatMessage(objectMapper.readValue(json, RedisMessageDTO.class)));
                }
            }
        }
        else if (offset < redisCount) {
            Set<String> redisMessagesJson = zSetOperations.reverseRange(redisKey, offset, fetchLimit - 1);

            if (redisMessagesJson != null && !redisMessagesJson.isEmpty()) {
                for (String json : redisMessagesJson) {
                    combinedMessages.add(translateRedisDtoIntoChatMessage(objectMapper.readValue(json, RedisMessageDTO.class)));
                }
            }

            int remainingLimit = fetchLimit - combinedMessages.size();
            if (remainingLimit > 0) {
                Sort sort = Sort.by(Sort.Direction.DESC, "sentAt");
                PageRequest pageRequest = PageRequest.of(0, remainingLimit, sort);

                List<ChatMessage> dbMessages = chatMessageRepository.findByChatRoom_ChatRoomId(chatRoomId, pageRequest);
                combinedMessages.addAll(dbMessages);
            }
        }
        else {  // redisCount <= offset
            long offsetForMysql = offset - redisCount;
            int pageForMysql = (int) (offsetForMysql / (pageable.getSize()));

            Sort sort = Sort.by(Sort.Direction.DESC, "sentAt");
            PageRequest pageRequest = PageRequest.of(pageForMysql, fetchLimit, sort);

            combinedMessages.addAll(chatMessageRepository.findByChatRoom_ChatRoomId(chatRoomId, pageRequest));
        }

        combinedMessages.sort(Comparator.comparing(ChatMessage::getSentAt));

        boolean hasNext = combinedMessages.size() > offset + pageable.getSize();
        int end = Math.min(offset + pageable.getSize(), combinedMessages.size());

        List<ReceiveMessageResponseDTO> pagedMessages = new ArrayList<>();
        if (offset < combinedMessages.size()) {
            pagedMessages = combinedMessages.subList(offset, end).stream()
                    .map(ReceiveMessageResponseDTO::fromChatMessageEntity)
                    .map(this::setReceiveMessageField)
                    .toList();
        }

        return new SliceImpl<>(pagedMessages, PageRequest.of(pageable.getPage(), pageable.getSize()), hasNext);
    }
}
