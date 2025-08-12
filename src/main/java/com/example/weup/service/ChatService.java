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

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    // basic chat message
    @Transactional
    public void sendBasicMessage(Long chatRoomId, SendMessageRequestDTO messageRequestDTO) throws JsonProcessingException {

        ChatRoom chatRoom = chatValidator.validateChatRoom(chatRoomId);

        Member sendMember = memberValidator.validateMemberAndProject(messageRequestDTO.getSenderId());
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, sendMember, true);

        checkAndSendDateChangeMessage(chatRoomId, LocalDateTime.now());

        RedisMessageDTO basicMessage = RedisMessageDTO.builder()
                .chatRoomId(chatRoomId)
                .uuid(UUID.randomUUID().toString())
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
                .uuid(UUID.randomUUID().toString())
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

        log.debug("Send System Message 부분으로 넘어옴");
        chatValidator.validateChatRoom(chatRoomId);

        checkAndSendDateChangeMessage(chatRoomId, LocalDateTime.now());

        RedisMessageDTO systemMessage = RedisMessageDTO.builder()
                .chatRoomId(chatRoomId)
                .uuid(UUID.randomUUID().toString())
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
                .uuid(UUID.randomUUID().toString())
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

        log.debug("Send Save Message 부분으로 넘어옴");
        String saveMessageKey = "chat:room:" + chatRoomId;
        redisTemplate.opsForZSet().add(saveMessageKey, objectMapper.writeValueAsString(message), message.getSentAt().toEpochSecond(ZoneOffset.UTC));

        ReceiveMessageResponseDTO receiveMessageResponseDto = ReceiveMessageResponseDTO.fromRedisMessageDTO(message);
        setReceiveMessageField(receiveMessageResponseDto);

        String readMessageKey = "chat:room:" + receiveMessageResponseDto.getUuid();
        // todo. 세션 정보 가져와서 읽은 사람 추가 로직 필요함. (아마 함수로 뺴야 할 듯)
        redisTemplate.opsForSet().add(readMessageKey, objectMapper.writeValueAsString(receiveMessageResponseDto.getSenderId()));

        int totalMemberCount = chatRoomMemberRepository.countByChatRoom_ChatRoomId(chatRoomId);
        Long readCount = redisTemplate.opsForSet().size(readMessageKey);
        if (readCount == null) readCount = 0L;
        int unreadCount = (int) (totalMemberCount - readCount);
        receiveMessageResponseDto.setUnreadCount(unreadCount);

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
                    .uuid(UUID.randomUUID().toString())
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

        // uuid ?
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
        int end = offset + pageable.getSize();
        int fetchLimit = pageable.getSize() + 1;

        Long redisCount = zSetOperations.size(redisKey);
        if (redisCount == null) redisCount = 0L;
        log.info("초기 설정 값 확인");
        log.info("redisKey : {}, offset : {}, end : {}, redisCount : {}", redisKey, offset, end, redisCount);

        List<ChatMessage> combinedMessages = new ArrayList<>();

        if (offset + fetchLimit <= redisCount) {
            log.info("첫 번째 케이스, Redis에서만 데이터를 가져옴");
            Set<String> redisMessagesJson = zSetOperations.reverseRange(redisKey, offset, end); // 0~20 (21개 가져옴)

            if (redisMessagesJson != null && !redisMessagesJson.isEmpty()) {
                log.info("첫 번째 케이스 - Redis Messages Json 개수 확인 : {}", redisMessagesJson.size());
                for (String json : redisMessagesJson) {
                    combinedMessages.add(translateRedisDtoIntoChatMessage(objectMapper.readValue(json, RedisMessageDTO.class)));
                }
            }
        }
        else if (offset < redisCount) {
            log.info("두 번째 케이스, Redis, MySQL에서 데이터를 가져옴");
            Set<String> redisMessagesJson = zSetOperations.reverseRange(redisKey, offset, end - 1);

            if (redisMessagesJson != null && !redisMessagesJson.isEmpty()) {
                log.info("두 번째 케이스 - Redis Messages Json 개수 확인 : {}", redisMessagesJson.size());
                for (String json : redisMessagesJson) {
                    combinedMessages.add(translateRedisDtoIntoChatMessage(objectMapper.readValue(json, RedisMessageDTO.class)));
                }
            }

            int remainingLimit = fetchLimit - combinedMessages.size();
            log.info("두 번째 케이스, MySQL에서 가져와야 하는 값 개수 확인 : {}", remainingLimit);

            if (remainingLimit > 0) {
                List<ChatMessage> dbMessages = chatMessageRepository.findMessagesByChatRoomIdWithOffset(chatRoomId, remainingLimit, 0);
                log.info("두 번째 케이스, 실제로 MySQL에서 가져온 메시지 개수 확인 : {}", dbMessages.size());
                combinedMessages.addAll(dbMessages);
            }
        }
        else {  // redisCount <= offset
            log.info("세 번째 케이스, MySQL에서만 데이터를 가져옴");
            int offsetForMysql = (int) (offset - redisCount);
            log.info("세 번째 케이스, 실제로 MySQL에서 가져와야 하는 offset 값 확인");
            log.info("offset for mysql : {}", offsetForMysql);

            combinedMessages.addAll(chatMessageRepository.findMessagesByChatRoomIdWithOffset(chatRoomId, fetchLimit, offsetForMysql));
        }

        log.info("분기 종료 후 실제로 들고온 데이터 개수 확인 : {}", combinedMessages.size());
        combinedMessages.sort(Comparator.comparing(ChatMessage::getSentAt).reversed());

        boolean hasNext = combinedMessages.size() > pageable.getSize();
        end = Math.min(pageable.getSize(), combinedMessages.size());
        log.info("정리해야 하는 값 확인, hasNext : {}, end : {}", hasNext, end);

        List<ReceiveMessageResponseDTO> pagedMessages = new ArrayList<>();
        if (!combinedMessages.isEmpty()) {
            pagedMessages = combinedMessages.subList(0, end).stream()
                    .map(ReceiveMessageResponseDTO::fromChatMessageEntity)
                    .map(this::setReceiveMessageField)
                    .sorted(Comparator.comparing(ReceiveMessageResponseDTO::getSentAt))
                    .toList();
        }

        log.info("\n\n\n마지막 확인");
        for (ReceiveMessageResponseDTO receiveMessageDTO : pagedMessages) {
            log.info("chat message 내역 확인 : {}", receiveMessageDTO.getMessage());
        }
        log.info("\n\n\n");

        return new SliceImpl<>(pagedMessages, PageRequest.of(pageable.getPage(), pageable.getSize()), hasNext);
    }
}
