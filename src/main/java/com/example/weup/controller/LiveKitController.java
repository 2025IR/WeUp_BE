package com.example.weup.controller;

import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.ResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.LiveKitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/meeting")
public class LiveKitController {

    private final JwtUtil jwtUtil;

    private final LiveKitService liveKitService;

    @PostMapping("/enter/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> enterRoom(@PathVariable Long projectId, HttpServletRequest request) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        String liveKitToken = liveKitService.generateLiveKitToken(projectId, userId);

        liveKitService.enterRoom(projectId, userId);

        return ResponseEntity
                .ok()
                .body(DataResponseDTO.of(liveKitToken, "livekit token 발급"));
    }

    @PostMapping("/leave/{projectId}")
    public ResponseEntity<ResponseDTO> leaveRoom(@PathVariable Long projectId, HttpServletRequest request) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        liveKitService.leaveRoom(projectId, userId);

        return ResponseEntity
                .ok()
                .body(new ResponseDTO(true, "회의실 연결이 종료되었습니다."));
    }

    @GetMapping("/count/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> getRoomUserCount(@PathVariable Long projectId, HttpServletRequest request) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Long count = liveKitService.getRoomUserCount(projectId, userId);

        return ResponseEntity
                .ok()
                .body(DataResponseDTO.of(String.valueOf(count), "화상 회의실 현재 참여 인원 수 : " + projectId));
    }

}
