package com.example.weup.handler;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.User;
import com.example.weup.repository.UserRepository;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.SessionService;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    private final SessionService sessionService;

    private final UserRepository userRepository;

    private final MemberValidator memberValidator;

    private static final Pattern PROJECT_TOPIC_PATTERN = Pattern.compile("^/topic/project/(\\d+)(/.*)?$");

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {

        Long userId = 0L;
        String destination;

        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {

            List<String> header = accessor.getNativeHeader("Authorization");
            if (header == null || header.isEmpty()) {
                log.warn("CONNECT - Authorization 헤더 없음");
                throw new GeneralException(ErrorInfo.UNAUTHORIZED);
            }

            String token = header.getFirst();
            if (jwtUtil.isExpired(token)) {
                log.warn("CONNECT - JWT 만료");
                throw new GeneralException(ErrorInfo.UNAUTHORIZED);
            }

            userId = jwtUtil.getUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            accessor.setUser(authToken);

            log.info("STOMP CONNECT from User - {}, Session - {}", userId, accessor.getSessionId());
        }

        if (command == null) {
            return message;
        }

        switch (command) {
            case SUBSCRIBE:
                destination = accessor.getDestination();
                if (destination == null) {
                    log.warn("SUBSCRIBE command receive with Null Destination from Session Id - {}", accessor.getSessionId());
                    log.warn("SUBSCRIBE command receive with Null Destination - {}", accessor.getDestination());
                    throw new GeneralException(ErrorInfo.WEBSOCKET_BAD_REQUEST);
                }
                // 개인 알림 진입
                if (destination.startsWith("/user/queue/notification")) {
                    log.info("Personal notification Subscribe -> Success : User - {}", userId);
                }
                // 프로젝트 알림 진입
                else if (destination.startsWith("/topic/project")) {
                    Long targetEntityId = null;
                    Matcher projectMatcher = PROJECT_TOPIC_PATTERN.matcher(destination);

                    if (projectMatcher.matches()) {
                        targetEntityId = Long.parseLong(projectMatcher.group(1));
                    }

                    if (targetEntityId != null) {
                        memberValidator.validateActiveMemberInProject(userId, targetEntityId);
                        log.info("Topic(Project) Subscribe -> Success : User - {}, Destination - {}", userId, destination);
                    }
                    else {
                        log.warn("Topic(Project) Subscribe -> Failure : User - {}, Destination - {}", userId, destination);
                    }
                }
                // 채팅방 알림 진입
                else if (destination.startsWith("/topic/chat")) {
                    long chatRoomId;

                    if (destination.split("/")[3].equals("active")) {
                        chatRoomId = Long.parseLong(destination.split("/")[4]);
                        log.info("Topic(Chatroom Active) Subscribe -> Success : User - {}, Destination - {}, chat room id - {}", userId, destination, chatRoomId);
                    }
                    else if (destination.split("/")[3].equals("connect")) {
                        chatRoomId = Long.parseLong(destination.split("/")[4]);
                        log.debug("split 확인 해보자 : {}", Arrays.toString(destination.split("/")));
                        log.info("Topic(Chatroom Connect) Subscribe -> Success : User - {}, Destination - {}, chat room id - {}", userId, destination, chatRoomId);
                        sessionService.addConnectMemberToChatRoom(chatRoomId, userId);
                    }
                    else {
                        log.info("Topic(Chatroom) Subscribe -> Failure : User - {}, Destination - {}", userId, destination);
                    }
                }
                break;

            case SEND:
                destination = accessor.getDestination();
                if (destination == null) {
                    log.warn("SUBSCRIBE command receive with Null Destination from Session Id - {}", accessor.getSessionId());
                    log.warn("SUBSCRIBE command receive with Null Destination - {}", accessor.getDestination());
                    throw new GeneralException(ErrorInfo.WEBSOCKET_BAD_REQUEST);
                }
                if (destination.startsWith("/app/send") || destination.startsWith("/app/project") ||
                        destination.startsWith("/app/todo") || destination.startsWith("/app/chat") || destination.startsWith("/app/schedule")) {
                    log.info("SEND Destination Validate -> Success : User - {}, Destination - {}", userId, destination);
                }
                else {
                    log.warn("SEND Destination Validate-> Failure : User - {}, Destination - {}", userId, destination);
                }
                break;

            case UNSUBSCRIBE:
                destination = accessor.getDestination();
                if (destination == null) {
                    log.warn("SUBSCRIBE command receive with Null Destination from Session Id - {}", accessor.getSessionId());
                    log.warn("SUBSCRIBE command receive with Null Destination - {}", accessor.getDestination());
                    throw new GeneralException(ErrorInfo.WEBSOCKET_BAD_REQUEST);
                }
                if (destination.startsWith("/topic/chat/active")) {
                    Long chatRoomId = Long.valueOf(destination.split("/")[4]);
                    sessionService.removeActiveMemberFromChatRoom(chatRoomId, userId);
                    sessionService.saveLastReadAt(chatRoomId, userId, Instant.now());
                    log.info("Topic(Chatroom Active) Unsubscribe -> Success : User - {}, Destination - {}", userId, destination);
                }
                else if (destination.startsWith("/topic/chat/connect")) {
                    Long chatRoomId = Long.valueOf(destination.split("/")[4]);
                    sessionService.removeConnectMemberFromChatRoom(chatRoomId, userId);
                    log.info("Topic(Chatroom Connect) Unsubscribe -> Success : User - {}, Destination - {}", userId, destination);
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
