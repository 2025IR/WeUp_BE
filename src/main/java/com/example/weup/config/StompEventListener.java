package com.example.weup.config;

import com.example.weup.entity.User;
import com.example.weup.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompEventListener {

    private final SessionService sessionService;

    @EventListener
    public void handlerWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        System.out.println("sessionId:" + sessionId);

        Authentication authentication = (Authentication) accessor.getUser();
        log.info("New WebSocket Connect : sessionId - {}", sessionId);

        if(authentication != null && authentication.isAuthenticated()){
            User user = (User) authentication.getPrincipal();
            String userId = String.valueOf(user.getUserId());

            sessionService.save(sessionId, userId);
            log.info("WebSocket Connect -> Success : user Id - {}, session Id - {}", userId, sessionId);
        } else {
            log.warn("WebSocket Connect -> Fail : session Id - {}", sessionId);
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
