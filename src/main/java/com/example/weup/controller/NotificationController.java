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

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public ResponseEntity<DataResponseDTO<Page<NotificationResponseDTO>>> getNotifications(
            @LoginUser Long userId, @PageableDefault(size = 20) Pageable pageable) {

        Page<NotificationResponseDTO> result = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(DataResponseDTO.of(result, ""));
    }
}
