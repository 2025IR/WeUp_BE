package com.example.weup.dto.response;

import com.example.weup.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponseDTO {

    private Long notificationId;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDTO from(Notification notification) {
        return NotificationResponseDTO.builder()
                .notificationId(notification.getNotificationId())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getNotificationCreatedAt())
                .build();
    }
}
