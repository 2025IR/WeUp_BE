package com.example.weup.controller;

import com.example.weup.dto.request.EditScheduleRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.ResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.ScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{projectId}")
    public ResponseEntity<DataResponseDTO<Map<Long, String>>> getSchedule(HttpServletRequest request, @PathVariable Long projectId) {

        jwtUtil.resolveToken(request);

        Map<Long, String> availableSchedule = scheduleService.getSchedule(projectId);

        return ResponseEntity
                .ok()
                .body(DataResponseDTO.of(availableSchedule, "프로젝트 팀원의 이용 가능 시간 출력 완료"));
    }

    @PutMapping("/edit")
    public ResponseEntity<ResponseDTO> editSchedule(HttpServletRequest request, @RequestBody EditScheduleRequestDTO editScheduleRequestDTO) {

        jwtUtil.resolveToken(request);

        scheduleService.editSchedule(editScheduleRequestDTO);

        return ResponseEntity
                .ok()
                .body(new ResponseDTO(true, "Available TIme 수정 : " + editScheduleRequestDTO.getMemberId()));
    }
}
