package com.example.weup.service;

import com.example.weup.entity.Member;
import com.example.weup.repository.ChatMessageRepository;
import com.example.weup.validate.ChatValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final StringRedisTemplate redisTemplate;

    private final ChatValidator chatValidator;

    private final ChatMessageRepository chatMessageRepository;

    private static final String SESSION_TO_USER_KEY = "ws:session";
    private static final String CHATROOM_ACTIVE_MEMBERS_KEY = "chatroom:%s:active:members";
    private static final String CHATROOM_CONNECT_MEMBERS_KEY = "chatroom:%s:connect:members";
    private static final String LAST_READ_AT_KEY = "chatroom:%s:lastReadAt:%s";

    // session connect
    public void saveSession(String sessionId, String userId) {
        redisTemplate.opsForValue().set(SESSION_TO_USER_KEY + sessionId, userId);
    }

    // session disconnect
    public void removeSession(String sessionId) {
        String userId = redisTemplate.opsForValue().get(SESSION_TO_USER_KEY + sessionId);
        if (userId != null) {
            redisTemplate.delete(SESSION_TO_USER_KEY + sessionId);
        }
    }

    // session id -> user id 조회
    public String getUserIdBySession(String sessionId) {
        return redisTemplate.opsForValue().get(SESSION_TO_USER_KEY + sessionId);
    }

    // 채팅방 active member 추가
    public void addActiveMemberToChatRoom(Long chatRoomId, Long userId) {
        Member member = chatValidator.validateMemberInChatRoomSession(chatRoomId, userId);
        redisTemplate.opsForSet().add(String.format(CHATROOM_ACTIVE_MEMBERS_KEY, chatRoomId), String.valueOf(member.getMemberId()));
    }

    // 채팅방 active member 제거
    public void removeActiveMemberFromChatRoom(Long chatRoomId, Long userId) {
        Member member = chatValidator.validateMemberInChatRoomSession(chatRoomId, userId);
        redisTemplate.opsForSet().remove(String.format(CHATROOM_ACTIVE_MEMBERS_KEY, chatRoomId), String.valueOf(member.getMemberId()));
    }

    // 채팅방 active member 목록 조회
    public Set<String> getActiveChatRoomMembers(Long chatRoomId) {
        return redisTemplate.opsForSet().members(String.format(CHATROOM_ACTIVE_MEMBERS_KEY, chatRoomId));
    }

    // 채팅방 active member 수 조회
    public long getActiveMembersCountInChatRoom(Long chatRoomId) {
        Long size = redisTemplate.opsForSet().size(String.format(CHATROOM_ACTIVE_MEMBERS_KEY, chatRoomId));
        return size != null ? size : 0;
    }

    // 채팅방 connect member 추가
    public void addConnectMemberToChatRoom(Long chatRoomId, Long userId) {
        Member member = chatValidator.validateMemberInChatRoomSession(chatRoomId, userId);
        redisTemplate.opsForSet().add(String.format(CHATROOM_CONNECT_MEMBERS_KEY, chatRoomId), String.valueOf(member.getMemberId()));
    }

    // 채팅방 connect member 제거
    public void removeConnectMemberFromChatRoom(Long chatRoomId, Long userId) {
        Member member = chatValidator.validateMemberInChatRoomSession(chatRoomId, userId);
        redisTemplate.opsForSet().remove(String.format(CHATROOM_CONNECT_MEMBERS_KEY, chatRoomId), String.valueOf(member.getMemberId()));
    }

    // 채팅방 connect member 목록 조회
    public Set<String> getConnectChatRoomMembers(Long chatRoomId) {
        return redisTemplate.opsForSet().members(String.format(CHATROOM_CONNECT_MEMBERS_KEY, chatRoomId));
    }

    // 채팅방 connect member 수 조회
    public long getConnectMembersCountInChatRoom(Long chatRoomId) {
        Long size = redisTemplate.opsForSet().size(String.format(CHATROOM_CONNECT_MEMBERS_KEY, chatRoomId));
        return size != null ? size : 0;
    }

    // lastReadAt 저장
    public void saveLastReadAt(Long chatRoomId, Long userId, Instant lastReadAt) {
        redisTemplate.opsForValue().set(String.format(LAST_READ_AT_KEY, chatRoomId, userId), String.valueOf(lastReadAt.toEpochMilli()));
    }

    // lastReadAt 불러오기
    public Instant getLastReadAt(Long chatRoomId, Long userId) {
        String lastReadAtStr = redisTemplate.opsForValue().get(String.format(LAST_READ_AT_KEY, chatRoomId, userId));
        if (lastReadAtStr == null) return null;
        return Instant.ofEpochMilli(Long.parseLong(lastReadAtStr));
    }

    public void processChatRoomEntry(Long chatRoomId, Long userId) {
        Instant lastReadAt = getLastReadAt(chatRoomId, userId);
        Instant startInstant = (lastReadAt == null) ? Instant.EPOCH : lastReadAt;

        Set<String> messageIds = getMessagesSentAfter(chatRoomId, startInstant);

        if (!messageIds.isEmpty()) {
            for (String messageId : messageIds) {
                String readMessageKey = "chat:" + messageId + ":readUsers";
                redisTemplate.opsForSet().add(readMessageKey, String.valueOf(userId));
            }
        }

        saveLastReadAt(chatRoomId, userId, Instant.now());
    }

    private Set<String> getMessagesSentAfter(Long chatRoomId, Instant lastReadAt) {
        Set<String> messageIds = new HashSet<>();
        String messageKey = "chat:room:" + chatRoomId;
        long minScore = lastReadAt.toEpochMilli();

        Set<String> redisMessageIds = redisTemplate.opsForZSet().rangeByScore(messageKey, minScore, Double.POSITIVE_INFINITY);
        if (redisMessageIds != null) {
            messageIds.addAll(redisMessageIds);
        }

        LocalDateTime lastReadLocalDateTime = LocalDateTime.ofInstant(lastReadAt, ZoneId.systemDefault());
        List<Long> mysqlMessageIds = chatMessageRepository.findMessageIdByChatRoom_ChatRoomIdAndSentAtAfter(chatRoomId, lastReadLocalDateTime);
        if (mysqlMessageIds != null) {
            messageIds.addAll(mysqlMessageIds.stream().map(String::valueOf).collect(Collectors.toSet()));
        }

        return messageIds;
    }
}
