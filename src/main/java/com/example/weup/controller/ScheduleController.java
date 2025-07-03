package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.EditScheduleRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.GetScheduleResponseDTO;
import com.example.weup.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/{projectId}")
    public ResponseEntity<DataResponseDTO<List<GetScheduleResponseDTO>>> getSchedule(@LoginUser Long userId,
                                                                                     @PathVariable Long projectId) {

        log.info("요청자 : {}, get all schedule -> start", userId);
        List<GetScheduleResponseDTO> availableSchedule = scheduleService.getSchedule(userId, projectId);

        log.info("요청자 : {}, get all schedule -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of(availableSchedule, "프로젝트 팀원 이용 가능 시간을 불러왔습니다."));
    }

    @PutMapping("/edit/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> editSchedule(@LoginUser Long userId, @PathVariable Long projectId,
                                                                @RequestBody EditScheduleRequestDTO editScheduleRequestDTO) {

        log.info("요청자 : {}, edit schedule -> start", userId);
        scheduleService.editSchedule(userId, projectId, editScheduleRequestDTO);

        log.info("요청자 : {}, edit schedule -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of("이용 가능 시간 수정이 완료되었습니다."));
    }
}
