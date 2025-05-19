package com.example.weup.controller;

import com.example.weup.security.JwtUtil;
import com.example.weup.service.ScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{projectId}")
    public ResponseEntity<Map<Long, String>> getSchedule(HttpServletRequest request, @PathVariable Long projectId) {

        String token = jwtUtil.resolveToken(request);

        scheduleService.getSchedule(projectId);
    }
}
