package com.example.weup.controller;

import com.example.weup.dto.request.ProjectCreateRequestDTO;
import com.example.weup.dto.request.ProjectEditRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.DetailProjectResponseDTO;
import com.example.weup.dto.response.ListUpProjectResponseDTO;
import com.example.weup.dto.response.ResponseDTO;
import com.example.weup.entity.Project;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.MemberService;
import com.example.weup.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    private final JwtUtil jwtUtil;

    private final MemberService memberService;

    @PostMapping("/create")
    public ResponseEntity<ResponseDTO> createProject(HttpServletRequest request,
                                                     @ModelAttribute ProjectCreateRequestDTO projectCreateRequestDTO) throws IOException {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Project newProject = projectService.createProject(projectCreateRequestDTO);
        memberService.addProjectCreater(userId, newProject);

        return ResponseEntity.ok(DataResponseDTO.of("프로젝트가 성공적으로 생성되었습니다."));
    }

    @PostMapping("/list")
    public ResponseEntity<DataResponseDTO<List<ListUpProjectResponseDTO>>> listUpProject(HttpServletRequest request) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        List<ListUpProjectResponseDTO> data = projectService.listUpProject(userId);

        return ResponseEntity.ok(DataResponseDTO.of(data, "프로젝트 목룍 조회가 완료되었습니다."));
    }

    @PostMapping("/detail/{projectId}")
    public ResponseEntity<DataResponseDTO<DetailProjectResponseDTO>> detailProject(HttpServletRequest request, @PathVariable Long projectId) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        DetailProjectResponseDTO data = projectService.detailProject(projectId, userId);

        return ResponseEntity.ok(DataResponseDTO.of(data, "프로젝트 상세 정보 조회가 완료되었습니다."));
    }

    @PutMapping("/edit/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> editProject(HttpServletRequest request, @PathVariable Long projectId,
                                                   @ModelAttribute ProjectEditRequestDTO dto) throws IOException {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        projectService.editProject(userId, projectId, dto);

        return ResponseEntity.ok(DataResponseDTO.of("프로젝트 정보 수정이 완료되었습니다."));
    }

    @PutMapping("/edit/description/{projectId}")
    public ResponseEntity<ResponseDTO> editProjectDescription(HttpServletRequest request, @PathVariable Long projectId,
                                                              @RequestParam String description) {

        jwtUtil.resolveToken(request);

        projectService.editProjectDescription(projectId, description);

        return ResponseEntity.ok(DataResponseDTO.of("프로젝트 설명 수정이 완료되었습니다."));
    }

}