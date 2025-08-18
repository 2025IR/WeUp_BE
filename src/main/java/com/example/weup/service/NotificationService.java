package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.NotificationResponseDTO;
import com.example.weup.entity.Member;
import com.example.weup.entity.Notification;
import com.example.weup.entity.Project;
import com.example.weup.entity.User;
import com.example.weup.repository.NotificationRepository;
import com.example.weup.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Page<NotificationResponseDTO> getNotifications(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        Page<Notification> notifications = notificationRepository
                .findByUserOrderByIsReadAscNotificationCreatedAtDesc(user, pageable);

        Page<NotificationResponseDTO> result = notifications.map(NotificationResponseDTO::from);

        notifications.forEach(Notification::markAsRead);

        return result;
    }

    @Transactional
    public int getUnreadNotificationCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        notificationRepository.deleteByIsReadTrueAndNotificationCreatedAtBefore(threshold);
    }

    public void sendPersonalNotification(User user, String message) {
        Notification notification = notificationRepository.save(
                Notification.builder()
                        .user(user)
                        .message(message)
                        .build()
        );

        messagingTemplate.convertAndSend(
                "/topic/user/" + user.getUserId(),
                Map.of(
                        "message", notification.getMessage(),
                        "createdAt", notification.getNotificationCreatedAt()
                )
        );
    }

    public void broadcastProjectNotification(Project project, String message, List<Long> excludeUserIds) {
        List<Member> members = project.getMembers().stream()
                .filter(m -> !m.isMemberDeleted() && (excludeUserIds == null || !excludeUserIds.contains(m.getUser().getUserId())))
                .collect(Collectors.toList());

        if (members.isEmpty()) return;

        List<Notification> notifications = members.stream()
                .map(m -> Notification.builder()
                        .user(m.getUser())
                        .message(message)
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);

        messagingTemplate.convertAndSend(
                "/topic/project/" + project.getProjectId(),
                Map.of(
                        "message", message,
                        "createdAt", LocalDateTime.now()
                )
        );
    }
}