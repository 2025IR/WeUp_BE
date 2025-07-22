package com.example.weup.config;

import com.example.weup.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompEventListener {

    private final SessionService sessionService;

    @EventListener
    public void handlerWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userId = (String) accessor.getSessionAttributes().get("userId");

        log.info("New WebSocket Connect : sessionId - {}", sessionId);
        if(userId != null) {
            sessionService.save(sessionId, userId);
        }
    }

    @EventListener
    public void handlerWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        log.info("WebSocket Disconnect : sessionId - {}", sessionId);
        sessionService.remove(sessionId);
    }
}
