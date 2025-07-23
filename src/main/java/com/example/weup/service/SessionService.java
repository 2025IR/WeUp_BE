package com.example.weup.service;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
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
        String removed = sessionIdToUserMap.remove(sessionId);
    }

    public String getUserId(String sessionId) {
        return sessionIdToUserMap.get(sessionId);
    }

    public Collection<String> getOnlineUsers() {
        return sessionIdToUserMap.values();
    }
}
