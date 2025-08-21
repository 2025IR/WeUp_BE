package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.NotificationResponseDTO;
import com.example.weup.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public ResponseEntity<DataResponseDTO<Page<NotificationResponseDTO>>> getNotifications(
            @LoginUser Long userId, @PageableDefault(size = 20) Pageable pageable) {

        Page<NotificationResponseDTO> result = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(DataResponseDTO.of(result, "알림 목록 조회가 완료되었습니다."));
    }

    @GetMapping("/unread")
    public ResponseEntity<DataResponseDTO<Map<String, Integer>>> getUnreadNotificationCount(@LoginUser Long userId) {
        int unreadCount = notificationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(DataResponseDTO.of(Map.of("unread", unreadCount), "읽지 않은 알림 수 조회가 완료되었습니다."));
    }
}
