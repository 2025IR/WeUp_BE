package com.example.weup.controller;

import com.example.weup.dto.request.EditScheduleRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetScheduleResponseDTO;
import com.example.weup.dto.response.ResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.ScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{projectId}")
    public ResponseEntity<DataResponseDTO<List<GetScheduleResponseDTO>>> getSchedule(HttpServletRequest request, @PathVariable Long projectId) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        List<GetScheduleResponseDTO> availableSchedule = scheduleService.getSchedule(userId, projectId);

        return ResponseEntity
                .ok()
                .body(DataResponseDTO.of(availableSchedule, "프로젝트 팀원의 이용 가능 시간 출력 완료"));
    }

    @PutMapping("/edit/{projectId}")
    public ResponseEntity<ResponseDTO> editSchedule(HttpServletRequest request, @PathVariable Long projectId, @RequestBody EditScheduleRequestDTO editScheduleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        scheduleService.editSchedule(userId, projectId, editScheduleRequestDTO);

        return ResponseEntity
                .ok()
                .body(new ResponseDTO(true, "Available Time 수정 : " + userId + ", " + projectId));
    }
}
