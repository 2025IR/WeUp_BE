package com.example.weup.config;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.User;
import com.example.weup.repository.UserRepository;
import com.example.weup.security.JwtUtil;
import com.example.weup.validate.MemberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;

    private final MemberValidator memberValidator;

    private static final Pattern PROJECT_TOPIC_PATTERN = Pattern.compile("^/topic/project/(\\d+)(/.*)?$");
    private static final Pattern CHATROOM_TOPIC_PATTERN = Pattern.compile("^/topic/chatroom/(\\d+)$");

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        String token = String.valueOf(Objects.requireNonNull(Objects.requireNonNull(accessor).getNativeHeader("Authorization")).getFirst());

        if (token == null) {
            log.warn("Stomp Channel Interceptor 내부 - Authorization 헤더 없음");
            throw new GeneralException(ErrorInfo.UNAUTHORIZED);
        }

        if (jwtUtil.isExpired(token)) {
            log.warn("Stomp Channel Interceptor 내부 - JWT 만료");
            throw new GeneralException(ErrorInfo.UNAUTHORIZED);
        }

        Long userId = jwtUtil.getUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        accessor.setUser(authToken);

        StompCommand command = accessor.getCommand();
        String destination = accessor.getDestination();

        switch (Objects.requireNonNull(command)) {
            case CONNECT:
                log.info("STOMP CONNECT from User - {}, Session - {}", userId, accessor.getSessionId());
                break;

            case SUBSCRIBE:
                if (destination == null) {
                    log.warn("SUBSCRIBE command receive with Null Destination from Session Id - {}", accessor.getSessionId());
                    throw new GeneralException(ErrorInfo.BAD_REQUEST);
                }

                if (destination.startsWith("/user/queue/notification")) {
                    log.info("Personal notification Subscribe -> Success : User - {}", userId);
                }
                else if (destination.startsWith("/topic/project")) {
                    Long targetEntityId = null;
                    Matcher chatRoomMatcher = CHATROOM_TOPIC_PATTERN.matcher(destination);
                    Matcher projectMatcher = PROJECT_TOPIC_PATTERN.matcher(destination);

                    if (chatRoomMatcher.matches()) {
                        targetEntityId = Long.parseLong(chatRoomMatcher.group(1));
                    }
                    else if (projectMatcher.matches()) {
                        targetEntityId = Long.parseLong(projectMatcher.group(1));
                    }

                    if (targetEntityId != null) {
                        memberValidator.validateActiveMemberInProject(userId, targetEntityId);
                        // TODO. 지금은 projectId = chatRoomId 라서 이게 통하는데 나중에 확장하면 바꿔야 함.
                        log.info("Topic Subscribe -> Success : User - {}, Destination - {}", userId, destination);
                    }
                    else {
                        log.warn("Topic Subscribe -> Failure : User - {}, Destination - {}", userId, destination);
                    }
                }
                break;

            case SEND:
                if (destination == null) {
                    log.warn("SEND command receive with Null Destination from Session Id - {}", accessor.getSessionId());
                }
                else if (destination.startsWith("/app/send") || destination.startsWith("/app/project") ||
                        destination.startsWith("/app/todo") || destination.startsWith("/app/chat") || destination.startsWith("/app/schedule")) {
                    log.info("SEND Destination Validate -> Success : User - {}, Destination - {}", userId, destination);
                }
                else {
                    log.warn("SEND Destination Validate-> Failure : User - {}, Destination - {}", userId, destination);
                }
                break;

            case DISCONNECT:
                log.info("STOMP DISCONNECT from User - {}, Session - {}", userId, accessor.getSessionId());
                break;

            default:
                log.info("STOMP Command Passed : command - {}", command);
                break;
        }

        return message;
    }
}
