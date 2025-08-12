package com.example.weup.service;

import com.example.weup.entity.Member;
import com.example.weup.validate.ChatValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final StringRedisTemplate redisTemplate;

    private final ChatValidator chatValidator;

    private static final String SESSION_TO_USER_KEY = "ws:session";
    private static final String USER_TO_SESSIONS_KEY = "ws:user:%s:sessions";
    private static final String CHATROOM_ACTIVE_MEMBERS_KEY = "chatroom:%s:active:members";
    private static final String CHATROOM_CONNECT_MEMBERS_KEY = "chatroom:%s:connect:members";
    private static final String LAST_READ_AT_KEY = "chatroom:%s:lastReatAt:%s";

    // session connect
    public void saveSession(String sessionId, String userId) {
        redisTemplate.opsForValue().set(SESSION_TO_USER_KEY + sessionId, userId);
        redisTemplate.opsForSet().add(String.format(USER_TO_SESSIONS_KEY, userId), sessionId);
    }

    // session disconnect
    public void removeSession(String sessionId) {
        String userId = redisTemplate.opsForValue().get(SESSION_TO_USER_KEY + sessionId);
        if (userId != null) {
            redisTemplate.delete(SESSION_TO_USER_KEY + sessionId);
            redisTemplate.opsForSet().remove(String.format(USER_TO_SESSIONS_KEY, userId), sessionId);
        }
    }

    // session id -> user id 조회
    public String getUserIdBySession(String sessionId) {
        return redisTemplate.opsForValue().get(SESSION_TO_USER_KEY + sessionId);
    }

    // user id -> session ids 조회
    public Set<String> getSessionsByUserId(Long userId) {
        return redisTemplate.opsForSet().members(String.format(USER_TO_SESSIONS_KEY, userId));
    }

    // user 온라인 상태인지 확인
    public boolean isUserOnline(Long userId) {
        Long size = redisTemplate.opsForSet().size(String.format(USER_TO_SESSIONS_KEY, userId));
        return size != null && size > 0;
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
}
