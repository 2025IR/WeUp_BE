package com.example.weup.service;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SessionService {

    private final ConcurrentMap<String, String> sessionIdToUserMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> userToSessionIdMap = new ConcurrentHashMap<>();

    public void save(String sessionId, String userId) {
        sessionIdToUserMap.put(sessionId, userId);
        userToSessionIdMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void remove(String sessionId) {
        String userId = sessionIdToUserMap.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userToSessionIdMap.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userToSessionIdMap.remove(userId);
                }
            }
        }
    }

    public String getUserId(String sessionId) {
        return sessionIdToUserMap.get(sessionId);
    }

    public Set<String> getSessionsByUserId(String userId) {
        return userToSessionIdMap.getOrDefault(userId, Collections.emptySet());
    }

    public Collection<String> getOnlineUserIds() {
        return userToSessionIdMap.keySet();
    }

    public boolean isUserOnline(String userId) {
        return userToSessionIdMap.containsKey(userId) && !userToSessionIdMap.get(userId).isEmpty();
    }
}
