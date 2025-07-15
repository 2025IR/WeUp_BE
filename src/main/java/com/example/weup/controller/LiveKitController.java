package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.service.LiveKitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/meeting")
public class LiveKitController {

    private final LiveKitService liveKitService;

    @PostMapping("/enter/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> enterRoom(@LoginUser Long userId, @PathVariable Long projectId) {

        log.info("요청자 : {}, enter meeting room -> start", userId);
        String liveKitToken = liveKitService.generateLiveKitToken(projectId, userId);
        liveKitService.enterRoom(projectId, userId);

        log.info("요청자 : {}, enter meeting room -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of(liveKitToken, "LiveKit Token 발급이 완료되었습니다."));
    }

    @PostMapping("/leave/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> leaveRoom(@LoginUser Long userId, @PathVariable Long projectId) {

        log.info("요청자 : {}, leave metting room -> start", userId);
        liveKitService.leaveRoom(projectId, userId);

        log.info("요청자 : {}, leave metting room -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of("회의실 연결이 종료되었습니다."));
    }

    @GetMapping("/count/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> getRoomUserCount(@LoginUser Long userId, @PathVariable Long projectId) {

        log.info("요청자 : {}, get participant count -> start", userId);
        Long count = liveKitService.getParticipantCount(projectId, userId);

        log.info("요청자 : {}, get participant count -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of(String.valueOf(count), "화상 회의실 현재 참여 인원 수 조회가 완료되었습니다."));
    }

    @PostMapping("/webhook")
    public ResponseEntity<DataResponseDTO<String>> handleWebhook(@LoginUser Long userId, @RequestBody Map<String, Object> payload) {

        String event = (String) payload.get("event");
        liveKitService.handleWebhook(payload, event);

        return ResponseEntity.ok(DataResponseDTO.of("WebHook Event 성공"));
    }

}
