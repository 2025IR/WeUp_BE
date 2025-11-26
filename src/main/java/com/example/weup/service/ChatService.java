package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.DisplayType;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.constant.SenderType;
import com.example.weup.dto.request.GetPageable;
import com.example.weup.dto.request.SendImageMessageRequestDTO;
import com.example.weup.dto.request.SendMessageRequestDTO;
import com.example.weup.dto.response.EnterChatRoomResponseDTO;
import com.example.weup.dto.response.ReceiveMessageResponseDTO;
import com.example.weup.dto.response.ReceiveMessageToConnectResponseDTO;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService{

    private final S3Service s3Service;

    private final SessionService sessionService;

    private final MemberRepository memberRepository;

    private final ReadMembersRepository readMembersRepository;

    private final ChatMessageRepository chatMessageRepository;

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    private final ObjectMapper objectMapper;

    private final StringRedisTemplate redisTemplate;

    private final SimpMessagingTemplate messagingTemplate;

    private final ChatValidator chatValidator;

    private final MemberValidator memberValidator;

    // basic chat message
    @Transactional
    public void sendBasicMessage(Long chatRoomId, SendMessageRequestDTO messageRequestDTO, Boolean isPrompt) throws JsonProcessingException {

        ChatRoom chatRoom = chatValidator.validateChatRoom(chatRoomId);

        log.debug("member validator - validate member and project 진입");
        Member sendMember = memberValidator.validateMemberAndProject(messageRequestDTO.getSenderId());
        log.debug("member validator - is member already in chat room 진입");
        memberValidator.isMemberAlreadyInChatRoom(chatRoom, sendMember, true);
        log.debug("member validator 탈출");

        checkAndSendDateChangeMessage(chatRoomId, LocalDateTime.now());

        RedisMessageDTO basicMessage = RedisMessageDTO.builder()
                .chatRoomId(chatRoomId)
                .uuid(UUID.randomUUID().toString())
                .memberId(sendMember.getMemberId())
                .message(messageRequestDTO.getMessage())
                .isImage(messageRequestDTO.getIsImage())
                .isPrompt(isPrompt)
                .sentAt(LocalDateTime.now())
                .displayType(setDisplayType(chatRoomId, messageRequestDTO.getSenderId(), SenderType.MEMBER, messageRequestDTO.getSentAt()))
                .build();

        saveMessage(chatRoomId, basicMessage);
        log.debug("send basic message -> end, member id : {}", sendMember.getMemberId());
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
                .isPrompt(false)
                .sentAt(LocalDateTime.now())
                .displayType(setDisplayType(chatRoomId, sendMember.getMemberId(), SenderType.MEMBER, LocalDateTime.now()))
                .build();

        saveMessage(chatRoomId, imgMessage);
        log.debug("send image message -> end, member id : {}", sendMember.getMemberId());
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
                .isPrompt(false)
                .sentAt(LocalDateTime.now())
                .senderType(SenderType.SYSTEM)
                .displayType(DisplayType.DEFAULT)
                .build();

        saveMessage(chatRoomId, systemMessage);
        log.debug("send system message -> end");
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
                .isPrompt(false)
                .sentAt(LocalDateTime.now())
                .senderType(SenderType.AI)
                .displayType(setDisplayType(chatRoomId, null, SenderType.AI, LocalDateTime.now()))
                .originalMessage(originalUserMessage)
                .originalSenderName(originalSenderName)
                .build();

        saveMessage(chatRoomId, aiMessage);
        log.debug("send ai message -> end, message : {}", message);
    }

    // send message
    private void saveMessage(Long chatRoomId, RedisMessageDTO message) throws JsonProcessingException {

        log.debug("\n Send Save Message IN");
        String saveMessageKey = "chat:room:" + chatRoomId;
        long sentAtMilli = message.getSentAt().atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        redisTemplate.opsForZSet().add(saveMessageKey, objectMapper.writeValueAsString(message), sentAtMilli);

        ReceiveMessageResponseDTO receiveMessageResponseDto = ReceiveMessageResponseDTO.fromRedisMessageDTO(message);
        setReceiveMessageField(receiveMessageResponseDto);

        String readMessageKey = "chat:" + receiveMessageResponseDto.getUuid() + ":readUsers";
        if (message.getSenderType() == SenderType.MEMBER) {
            log.debug("save message -> sender type = MEMBER 채팅만 read user 로직 실행");
            saveReadUser(readMessageKey, String.valueOf(receiveMessageResponseDto.getSenderId()));
        }

        int totalMemberCount = chatRoomMemberRepository.countByChatRoom_ChatRoomId(chatRoomId);
        long readCount = sessionService.getActiveMembersCountInChatRoom(chatRoomId);
        int unreadCount = (int) (totalMemberCount - readCount);
        receiveMessageResponseDto.setUnreadCount(unreadCount);

        log.error("*@#!@!#$@!$@!@#!!@#@#$@$    " + receiveMessageResponseDto.isPrompt());

        messagingTemplate.convertAndSend("/topic/chat/active/" + chatRoomId, receiveMessageResponseDto);
        log.debug("active member 에게 메시지 전송, destination : /topic/chat/active/" + chatRoomId);

        ReceiveMessageToConnectResponseDTO connectResponseDTO = ReceiveMessageToConnectResponseDTO.builder()
                .message(receiveMessageResponseDto.getMessage())
                .sentAt(receiveMessageResponseDto.getSentAt())
                .build();

        messagingTemplate.convertAndSend("/topic/chat/connect/" + chatRoomId, connectResponseDTO);
        log.debug("connect member 에게 메시지 전송, destination : /topic/chat/connect/" + chatRoomId);

        Set<String> connectMembers = sessionService.getActiveChatRoomMembers(chatRoomId);
        log.debug("\n read members 추가 시작");
        for (String memberId : connectMembers) {
            saveReadUser(readMessageKey, memberId);
        }

        log.debug("save message -> end");
    }

    private void saveReadUser(String saveKey, String memberId) {
        redisTemplate.opsForSet().add(saveKey, memberId);
        log.debug("read members 추가, read member key : {}, member id : {}", saveKey, memberId);
    }

    private DisplayType setDisplayType(Long chatRoomId, Long senderId, SenderType senderType, LocalDateTime sentAt) throws JsonProcessingException {

        ChatMessage lastMessage;
        String key = "chat:room:" + chatRoomId;

        Set<String> lastSet = redisTemplate.opsForZSet().range(key, -1, -1);
        String lastJson = (lastSet != null && !lastSet.isEmpty())
                ? lastSet.iterator().next()
                : null;

        if (lastJson != null) {
            RedisMessageDTO redisMessage = objectMapper.readValue(lastJson, RedisMessageDTO.class);
            lastMessage = translateRedisDtoIntoChatMessage(redisMessage);
        } else {
            lastMessage = chatMessageRepository.findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(chatRoomId);
        }

        if (lastMessage != null) {
            log.debug("\n\n set display type, lastMessage uuid : {}, message : {}", lastMessage.getUuid(), lastMessage.getMessage());

            if (lastMessage.getSenderType() == SenderType.MEMBER) {
                log.debug("member id가 존재할 떄, 즉, member type 메시지 일 때, member id : {}", lastMessage.getMember().getMemberId());
                if (lastMessage.getMember().getMemberId().equals(senderId)) {
                    log.debug("마지막 채팅의 member id, 지금 보낸 사람의 member id가 동일할 때");
                    if (isSameMinute(lastMessage.getSentAt(), sentAt)) {
                        log.debug("1 시간까지 같은가 ?");
                        return DisplayType.SAME_TIME;
                    } else {
                        log.debug("1 시람만 같은가 ?");
                        return DisplayType.SAME_SENDER;
                    }
                }
            } else {
                log.debug("이전 메시지가 system, ai, deleted 메시지 일 떄");
                if (lastMessage.getSenderType().equals(senderType)) {
                    log.debug("마지막 채팅의 sender type과 지금 메시지의 sender type이 동일할 때");
                    if (isSameMinute(lastMessage.getSentAt(), sentAt)) {
                        log.debug("2 시간까지 같은가 ?");
                        return DisplayType.SAME_TIME;
                    } else {
                        log.debug("2 사람만 같은가 ?");
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

        log.info("\n Check And Send Data Change Message IN");
        String key = "chat:room:" + chatRoomId;

        LocalDate currentDate = currentMessageTime.toLocalDate();
        LocalDate lastMessageDate = null;

        Set<String> lastMessages = redisTemplate.opsForZSet().range(key, -1, -1);
        if (lastMessages != null && !lastMessages.isEmpty()) {

            String lastMessage = lastMessages.iterator().next();
            RedisMessageDTO lastChatMessage = objectMapper.readValue(lastMessage, RedisMessageDTO.class);
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
                    .isPrompt(false)
                    .sentAt(currentMessageTime)
                    .senderType(SenderType.SYSTEM)
                    .displayType(DisplayType.DEFAULT)
                    .build();

            saveMessage(chatRoomId, systemMessage);
        }

        log.debug("check and send date change message -> end");
    }

    private void setReceiveMessageField(ReceiveMessageResponseDTO messageDTO) {

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

        List<ChatMessage> chatMessagesToSave = new ArrayList<>();

        for (String message : messages) {
            RedisMessageDTO redisMessage = objectMapper.readValue(message, RedisMessageDTO.class);
            ChatMessage chatMessage = translateRedisDtoIntoChatMessage(redisMessage);
            chatMessagesToSave.add(chatMessage);
        }

        List<ChatMessage> savedMessages = chatMessageRepository.saveAll(chatMessagesToSave);
        log.debug("flush chat message to db -> success : data size - {}", savedMessages.size());

        List<ReadMembers> readMembersToSave = new ArrayList<>();
        List<String> redisKeysToDelete = new ArrayList<>();

        for (ChatMessage savedMessage : savedMessages) {
            String uuid = savedMessage.getUuid();
            if (uuid == null) {
                log.debug("db flush 중 UUID 없는 이상한 Message 발견, message id - {}", savedMessage.getMessageId());
                continue;
            }

            String readUsersKey = "chat:" + uuid + ":readUsers";
            log.debug("db flush 중 read users key 값 확인 - {}", readUsersKey);

            if (savedMessage.getSenderType() == SenderType.SYSTEM) {
                redisKeysToDelete.add(readUsersKey);
                continue;
            }

            Set<String> readUserIds = redisTemplate.opsForSet().members(readUsersKey);

            if (readUserIds != null && !readUserIds.isEmpty()) {
                for (String memberIdStr : readUserIds) {
                    log.debug("db flush, read user flush 중 member id 확인 - {}", memberIdStr);
                    Long memberId = Long.parseLong(memberIdStr);
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

                    ReadMembers readMember = ReadMembers.builder()
                            .chatMessage(savedMessage)
                            .member(member)
                            .build();

                    readMembersToSave.add(readMember);
                }
            }
            redisKeysToDelete.add(readUsersKey);
        }

        readMembersRepository.saveAll(readMembersToSave);
        log.debug("flush message read members to db -> success : data size - {}", readMembersToSave.size());

        redisKeysToDelete.add(key);
        redisTemplate.delete(redisKeysToDelete);
        log.debug("flush chat data from redis -> success : deleted keys - {}", redisKeysToDelete);
    }

    public ChatMessage translateRedisDtoIntoChatMessage(RedisMessageDTO message) {

        ChatRoom chatRoom = chatValidator.validateChatRoom(message.getChatRoomId());

        return ChatMessage.builder()
                .uuid(message.getUuid())
                .chatRoom(chatRoom)
                .member(message.getMemberId() != null
                        ? memberRepository.findById(message.getMemberId()).orElseThrow(
                                () -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND))
                        : null)
                .message(message.getMessage())
                .isImage(message.getIsImage())
                .isPrompt(message.getIsPrompt())
                .sentAt(message.getSentAt())
                .senderType(message.getSenderType())
                .displayType(message.getDisplayType())
                .originalMessage(message.getOriginalMessage())
                .originalSenderName(message.getOriginalSenderName())
                .build();
    }

    private ReceiveMessageResponseDTO toReceiveMessageResponseDTO(ChatMessage chatMessage) {
        ReceiveMessageResponseDTO messageDTO = ReceiveMessageResponseDTO.fromChatMessageEntity(chatMessage);
        setReceiveMessageField(messageDTO);

        if (messageDTO.getUuid() == null) {
            throw new GeneralException(ErrorInfo.BAD_REQUEST);  // 나중에 에러 타입 수정
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey("chat:" + messageDTO.getUuid() + ":readUsers"))) {
            int totalMembersCount = chatRoomMemberRepository.countByChatRoom_ChatRoomId(chatMessage.getChatRoom().getChatRoomId());
            Set<String> readUsers = redisTemplate.opsForSet().members("chat:" + messageDTO.getUuid() + ":readUsers");
            long readCount = readUsers != null ? readUsers.size() : 0;
            int unreadMembersCount = (int) (totalMembersCount - readCount);

            messageDTO.setUnreadCount(unreadMembersCount);
        }
        else {
            int totalMembersCount = chatRoomMemberRepository.countByChatRoom_ChatRoomId(chatMessage.getChatRoom().getChatRoomId());
            Long readCount = readMembersRepository.countReadMembersByMessageId(chatMessage.getMessageId());
            int unreadMembersCount = (int) (totalMembersCount - readCount);

            messageDTO.setUnreadCount(unreadMembersCount);
        }

        return messageDTO;
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
                    .map(this::toReceiveMessageResponseDTO)
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

    public ChatMessage getLatestMessage(Long chatRoomId) throws JsonProcessingException {

        log.debug("\n get latest mesaage IN");
        String redisKey = "chat:room:" + chatRoomId;
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();

        Set<String> latestMessageJson = zSetOperations.reverseRange(redisKey, 0, 0);

        if (latestMessageJson != null && !latestMessageJson.isEmpty()) {
            String json = latestMessageJson.stream().findFirst().get();
            RedisMessageDTO redisMessage = objectMapper.readValue(json, RedisMessageDTO.class);
            log.debug("마지막 메시지 확인, uuid : {}, message : {}", redisMessage.getUuid(), redisMessage.getMessage());
            return translateRedisDtoIntoChatMessage(redisMessage);
        }
        else {
            log.debug("redis에 아무것도 없어서 mysql에서 가져옴");
            return chatMessageRepository.findTopByChatRoom_ChatRoomIdOrderBySentAtDesc(chatRoomId);
        }
    }

    public void enterChatRoomEvent(Long chatRoomId, Long userId) {

        log.debug("\n enter chat room event IN");
        Member member = chatValidator.validateMemberInChatRoomSession(chatRoomId, userId);
        Instant lastReadAt = sessionService.getLastReadAt(chatRoomId, userId);
        log.debug("Enter Chat Room Event -> {}", lastReadAt);

        EnterChatRoomResponseDTO enterChatRoomDTO = EnterChatRoomResponseDTO.builder()
                .memberId(member.getMemberId())
                .lastReadTime(lastReadAt)
                .build();

        messagingTemplate.convertAndSend("/topic/chat/active/" + chatRoomId, enterChatRoomDTO);
        log.debug("enter chat room event -> end");
    }

    @Transactional
    public void processChatRoomEntry(Long chatRoomId, Long userId) throws JsonProcessingException {
        Instant lastReadAt = sessionService.getLastReadAt(chatRoomId, userId);
        Instant startInstant = (lastReadAt == null) ? Instant.EPOCH : lastReadAt;

        updateReadRedisMessageUser(chatRoomId, userId, startInstant);
        updateReadDBMessageUser(chatRoomId, userId, startInstant);

        sessionService.saveLastReadAt(chatRoomId, userId, Instant.now());
    }

    private void updateReadRedisMessageUser(Long chatRoomId, Long userId, Instant lastReadAt) throws JsonProcessingException {
        String messageKey = "chat:room:" + chatRoomId;
        long minScore = lastReadAt.toEpochMilli();

        Member member = chatValidator.validateMemberInChatRoomSession(chatRoomId, userId);
        log.debug("update read redis message user -> member validator -> end");

        Set<String> redisMessages = redisTemplate.opsForZSet().rangeByScore(messageKey, minScore, Double.POSITIVE_INFINITY);
        if (redisMessages == null || redisMessages.isEmpty()) {
            log.debug("update read reids message user -> redis empty !!!");
            return;
        }

        for (String redisMessage : redisMessages) {
            RedisMessageDTO messageDTO = objectMapper.readValue(redisMessage, RedisMessageDTO.class);
            String readUsersKey = "chat:" + messageDTO.getUuid() + ":readUsers";

            log.debug("update read redis message user -> message uuid : {}, message : {}, readUsersKey : {}", messageDTO.getUuid(), messageDTO.getMessage(), readUsersKey);
            redisTemplate.opsForSet().add(readUsersKey, String.valueOf(member.getMemberId()));
            log.debug("update read redis message user -> SUCCESS");
        }
    }

    private void updateReadDBMessageUser(Long chatRoomId, Long userId, Instant startInstant) {
        LocalDateTime lastReadLocalDateTime = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault());
        Member member = chatValidator.validateMemberInChatRoomSession(chatRoomId, userId);
        log.debug("update read db message user -> member validator -> end");

        List<ChatMessage> mysqlMessages = chatMessageRepository.findChatMessageByChatRoom_ChatRoomIdAndSentAtAfter(chatRoomId, lastReadLocalDateTime);
        if (mysqlMessages == null || mysqlMessages.isEmpty()) {
            log.debug("update read db message user -> mysql empty !!!");
            return;
        }

        for (ChatMessage chatMessage : mysqlMessages) {
            log.debug("update read db message user -> message id : {}, message : {}", chatMessage.getMessageId(), chatMessage.getMessage());
            if (chatMessage.getSenderType() == SenderType.SYSTEM) continue;

            int returnValue = readMembersRepository.insertIgnore(chatMessage.getMessageId(), member.getMemberId());
            if (returnValue == 1) {
                log.debug("update read db message user -> SUCCESS, message id - {}, member id - {}", chatMessage.getMessageId(), member.getMemberId());
            } else {
                log.debug("update read db message user -> ERROR : 중복 값 존재, message id - {}, member id - {}", chatMessage.getMessageId(), member.getMemberId());
            }
        }
    }

}
