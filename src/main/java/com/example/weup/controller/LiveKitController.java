package com.example.weup.controller;

import com.example.weup.dto.request.UserIdRequestDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.LiveKitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/livekit")
public class LiveKitController {

    private final JwtUtil jwtUtil;

    private final LiveKitService liveKitService;

    @PostMapping("/enter/{projectId}")
    public ResponseEntity<Map<String, Object>> enterRoom(@PathVariable Long projectId, @RequestBody UserIdRequestDTO dto) {

//        String token = jwtUtil.resolveToken(request);
//        Long userId = jwtUtil.getUserId(token);

        String liveKitToken = liveKitService.generateLiveKitToken(projectId, dto.getUserId());

        liveKitService.enterRoom(projectId, dto.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("livekitToken", liveKitToken);

        return ResponseEntity.ok(response);
    }

    // 어떻게 user_id를 넘겨받을지..?
    @PostMapping("/leave/{projectId}")
    public ResponseEntity<String> leaveRoom(@PathVariable Long projectId, @RequestBody UserIdRequestDTO dto) {

        liveKitService.leaveRoom(projectId, dto.getUserId());

        return ResponseEntity.ok("퇴장 처리 완료");
    }

    @GetMapping("/count/{projectId}")
    public ResponseEntity<Long> getRoomUserCount(@PathVariable Long projectId) {
        Long count = liveKitService.getRoomUserCount(projectId);
        return ResponseEntity.ok(count);
    }

}
