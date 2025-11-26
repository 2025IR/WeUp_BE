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

        Long userId;
        String destination;

        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        if (command == null) return message;
        log.info("STOMP command = {}", command);

        switch (command) {
            case CONNECT:
                List<String> connectHeader = accessor.getNativeHeader("Authorization");
                if (connectHeader == null || connectHeader.isEmpty()) {
                    log.warn("CONNECT - Authorization 헤더 없음");
                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
                }

                String connectToken = connectHeader.getFirst();
                if (jwtUtil.isExpired(connectToken)) {
                    log.warn("CONNECT - JWT 만료");
                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
                }

                userId = jwtUtil.getUserId(connectToken);
                User connectUser = userRepository.findById(userId)
                        .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(connectUser, null, connectUser.getAuthorities());

                accessor.setUser(authToken);

                log.info("CONNECT from User - {}, Session - {}", userId, accessor.getSessionId());
                break;

            case SUBSCRIBE:
                destination = accessor.getDestination();
                if (destination == null) {
                    log.warn("SUBSCRIBE command receive with Null Destination from Session Id - {}", accessor.getSessionId());
                    throw new GeneralException(ErrorInfo.WEBSOCKET_BAD_REQUEST);
                }
                log.info("SUBSCRIBE destination = {}", destination);

                List<String> subscribeHeader = accessor.getNativeHeader("Authorization");
                if (subscribeHeader == null || subscribeHeader.isEmpty()) {
                    log.warn("SUBSCRIBE - Authorization 헤더 없음");
                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
                }

                String subscribeToken = subscribeHeader.getFirst();
                if (jwtUtil.isExpired(subscribeToken)) {
                    log.warn("SUBSCRIBE - JWT 만료");
                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
                }

                userId = jwtUtil.getUserId(subscribeToken);
                userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

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
                    log.info("Stomp 분기 처리 - Subscribe /topic/project : {}", targetEntityId);

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
                        log.debug("split check : {}", Arrays.toString(destination.split("/")));
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
                    log.warn("SEND command receive with Null Destination from Session Id - {}", accessor.getSessionId());
                    throw new GeneralException(ErrorInfo.WEBSOCKET_BAD_REQUEST);
                }
                log.info("SEND destination = {}", destination);

                List<String> sendHeader = accessor.getNativeHeader("Authorization");
                if (sendHeader == null || sendHeader.isEmpty()) {
                    log.warn("SEND - Authorization 헤더 없음");
                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
                }

                String sendToken = sendHeader.getFirst();
                if (jwtUtil.isExpired(sendToken)) {
                    log.warn("SEND - JWT 만료");
                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
                }

                userId = jwtUtil.getUserId(sendToken);
                userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

                if (destination.startsWith("/app/send") || destination.startsWith("/app/project") ||
                        destination.startsWith("/app/todo") || destination.startsWith("/app/chat") || destination.startsWith("/app/schedule")) {
                    log.info("SEND Destination Validate -> Success : User - {}, Destination - {}", userId, destination);
                }
                else {
                    log.warn("SEND Destination Validate-> Failure : User - {}, Destination - {}", userId, destination);
                }
                break;

            case UNSUBSCRIBE:
                List<String> unsubscribeHeader = accessor.getNativeHeader("Authorization");
                if (unsubscribeHeader == null || unsubscribeHeader.isEmpty()) {
                    log.warn("UNSUBSCRIBE - Authorization 헤더 없음");
                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
                }

                String unsubscribeToken = unsubscribeHeader.getFirst();
                if (jwtUtil.isExpired(unsubscribeToken)) {
                    log.warn("UNSUBSCRIBE - JWT 만료");
                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
                }

                userId = jwtUtil.getUserId(unsubscribeToken);
                userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));

                destination = accessor.getDestination();
                if (destination == null) {
                    log.warn("UNSUBSCRIBE command receive with Null Destination from Session Id - {}", accessor.getSessionId());
                    return message;
                }
                log.info("UNSUBSCRIBE destination = {}", destination);

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
//                List<String> disconnectHeader = accessor.getNativeHeader("Authorization");
//                if (disconnectHeader == null || disconnectHeader.isEmpty()) {
//                    log.warn("DISCONNECT - Authorization 헤더 없음");
//                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
//                }
//
//                String disconnectToken = disconnectHeader.getFirst();
//                if (jwtUtil.isExpired(disconnectToken)) {
//                    log.warn("DISCONNECT - JWT 만료");
//                    throw new GeneralException(ErrorInfo.UNAUTHORIZED);
//                }
//
//                userId = jwtUtil.getUserId(disconnectToken);
//                userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다."));
//                log.info("DISCONNECT from User - {}, Session - {}", userId, accessor.getSessionId());
                log.info("DISCONNECT from Session - {}", accessor.getSessionId());

                break;

            default:
                log.info("STOMP Command Passed : command - {}", command);
                break;
        }

        return message;
    }
}
