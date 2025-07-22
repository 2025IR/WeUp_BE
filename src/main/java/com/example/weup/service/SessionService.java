package com.example.weup.service;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();

    public void save(String sessionId, String userId) {
        sessions.put(sessionId, userId);
    }

    public void remove(String sessionId) {
        String removed = sessions.remove(sessionId);
    }

    public String getUserId(String sessionId) {
        return sessions.get(sessionId);
    }

    public Collection<String> getOnlineUsers() {
        return sessions.values();
    }
}
