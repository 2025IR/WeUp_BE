package com.example.weup.config;

import com.example.weup.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

@Slf4j
@RequiredArgsConstructor
public class CustomChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if(StompCommand.CONNECT.equals(accessor.getCommand())){
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null) {
                String userId = String.valueOf(jwtUtil.getUserId(token));

                log.info("✅ CONNECT: userId={}", userId);
                accessor.getSessionAttributes().put("userId", userId);
            } else {
                log.warn("⚠️ CONNECT 시 Authorization 헤더 없음!");
            }
        }

        if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
            log.info("SUBSCRIBE 요청: destination={}", accessor.getDestination());
        }

        if(StompCommand.DISCONNECT.equals(accessor.getCommand())){
            log.info("DISCONNECT 요청: sessionId={}", accessor.getSessionId());
        }

        return message;
    }
}
