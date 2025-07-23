package com.example.weup.config;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.User;
import com.example.weup.repository.UserRepository;
import com.example.weup.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if(StompCommand.CONNECT.equals(accessor.getCommand())){
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null) {
                if(jwtUtil.isExpired(token)) {
                    throw new GeneralException(ErrorInfo.TOKEN_EXPIRED);
                }

                Long userId = jwtUtil.getUserId(token);
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                accessor.setUser(authToken);
                SecurityContextHolder.getContext().setAuthentication(authToken);

            } else {
                throw new GeneralException(ErrorInfo.UNAUTHORIZED);
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
