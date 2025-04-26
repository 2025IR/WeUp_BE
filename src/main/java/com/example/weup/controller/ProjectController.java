package com.example.weup.controller;

import com.example.weup.dto.request.CreateProjectDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.DetailProjectResponseDTO;
import com.example.weup.dto.response.ListUpProjectResponseDTO;
import com.example.weup.dto.response.ResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    private final JwtUtil jwtUtil;

    // 프로젝트 생성
    @PostMapping("/create")
    public ResponseEntity<ResponseDTO> createProject(HttpServletRequest request, @RequestBody CreateProjectDTO createProjectDto) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);
        log.debug("token id: {}", userId);

        Long projectId = projectService.createProject(createProjectDto);
        log.debug("project id: {}", projectId);

        // 멤버 테이블에 팀장 추가하는 코드 필요

        return ResponseEntity
                .ok()
                .body(new ResponseDTO(true, "프로젝트 생성자 : " + userId));
    }

    // 프로젝트 리스트 불러오기. -> Member 코드가 있어야 가능함.
//    @PostMapping("/list")
//    public ResponseEntity<DataResponseDTO<ListUpProjectResponseDTO>> listUpProject(HttpServletRequest request) {
//
//        String token = jwtUtil.resolveToken(request);
//        Long userId = jwtUtil.getUserId(token);
//        log.debug("token id: {}", userId);
//
//        List<ListUpProjectResponseDTO> data = projectService.listUpProject(userId);
//
//    }

    // 프로젝트 상세 불러오기
    @PostMapping("/detail/{projectId}")
    public ResponseEntity<DataResponseDTO<DetailProjectResponseDTO>> detailProject(HttpServletRequest request, @PathVariable Long projectId) {

        jwtUtil.resolveToken(request);

        DetailProjectResponseDTO data = projectService.detailProject(projectId);

        return ResponseEntity
                .ok()
                .body(DataResponseDTO.of(data, "프로젝트 상세 정보 : " + projectId));
    }

    // 프로젝트 상태 변경
    @PutMapping("/change/state/{projectId}")
    public ResponseEntity<ResponseDTO> changeProjectState(HttpServletRequest request, @PathVariable Long projectId, @RequestParam Integer status) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);
        log.debug("token id: {}", userId);

        // 이 수정은 팀장만 가능한데, 해당 로직은 나중에 추가.

        projectService.changeProjectState(projectId, status);

        return ResponseEntity.ok()
                .body(new ResponseDTO(true, "프로젝트 상태 변경 : " + projectId));
    }

    // 프로젝트 수정
    @PutMapping("/edit/{projectId}")
    public ResponseEntity<ResponseDTO> editProject(HttpServletRequest request, @PathVariable Long projectId, @RequestBody CreateProjectDTO createProjectDto) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);
        log.debug("token id: {}", userId);

        // 이 수정은 팀장만 가능한데, 해당 로직은 나중에 추가.

        projectService.editProject(projectId, createProjectDto);

        return ResponseEntity.ok()
                .body(new ResponseDTO(true, "프로젝트 정보 수정 : " + projectId));
    }


    // 프로젝트 설명 수정
    @PutMapping("/edit/description/{projectId}")
    public ResponseEntity<ResponseDTO> editProjectDescription(HttpServletRequest request, @PathVariable Long projectId, @RequestParam String description) {

        jwtUtil.resolveToken(request);

        //String descriptionData = description.get("description");
        projectService.editProjectDescription(projectId, description);

        return ResponseEntity.ok()
                .body(new ResponseDTO(true, "프로젝트 설명 수정 : " + projectId));
    }

}