package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.ProjectCreateRequestDTO;
import com.example.weup.dto.request.ProjectDescriptionUpdateRequestDTO;
import com.example.weup.dto.request.ProjectEditRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.DetailProjectResponseDTO;
import com.example.weup.dto.response.ListUpProjectResponseDTO;
import com.example.weup.dto.response.ResponseDTO;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Member;
import com.example.weup.entity.Project;
import com.example.weup.service.ChatRoomService;
import com.example.weup.service.MemberService;
import com.example.weup.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    private final MemberService memberService;

    private final ChatRoomService chatRoomService;

    @PostMapping("/create")
    public ResponseEntity<ResponseDTO> createProject(@LoginUser Long userId,
                                                     @Valid @ModelAttribute ProjectCreateRequestDTO projectCreateRequestDTO) throws IOException {

        log.info("요청자 : {}, create project -> start", userId);
        Project newProject = projectService.createProject(projectCreateRequestDTO);
        Member newMember = memberService.addProjectCreater(userId, newProject);
        ChatRoom newChatRoom = chatRoomService.createBasicChatRoom(newProject, projectCreateRequestDTO.getProjectName());

        chatRoomService.addChatRoomMember(newChatRoom, newMember.getMemberId());

        log.info("요청자 : {}, create project -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of("프로젝트가 성공적으로 생성되었습니다."));
    }

    @PostMapping("/list")
    public ResponseEntity<DataResponseDTO<List<ListUpProjectResponseDTO>>> getProjectList(@LoginUser Long userId) {

        log.info("요청자 : {}, get project list -> start", userId);
        List<ListUpProjectResponseDTO> data = projectService.getProjectList(userId);

        log.info("요청자 : {}, get project list -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of(data, "프로젝트 목룍 조회가 완료되었습니다."));
    }

    @PostMapping("/detail/{projectId}")
    public ResponseEntity<DataResponseDTO<DetailProjectResponseDTO>> getProjectDetail(@LoginUser Long userId, @PathVariable Long projectId) {

        log.info("요청자 : {}, get project detail -> start", userId);
        DetailProjectResponseDTO data = projectService.getProjectDetail(projectId, userId);

        log.info("요청자 : {}, get project detail -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of(data, "프로젝트 상세 정보 조회가 완료되었습니다."));
    }

    @PutMapping("/edit/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> editProject(@LoginUser Long userId, @PathVariable Long projectId,
                                                   @ModelAttribute ProjectEditRequestDTO dto) throws IOException {

        log.info("요청자 : {}, edit project information -> start", userId);
        projectService.editProject(userId, projectId, dto);

        log.info("요청자 : {}, edit project information -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of("프로젝트 정보 수정이 완료되었습니다."));
    }

//    @PutMapping("/edit/description/{projectId}")
//    public ResponseEntity<DataResponseDTO<String>> editProjectDescription(@LoginUser Long userId, @PathVariable Long projectId,
//                                                              @RequestParam String description) {
//
//        log.info("요청자 : {}, edit project description -> start", userId);
//        projectService.editProjectDescription(userId, projectId, description);
//
//        log.info("요청자 : {}, edit project description -> success", userId);
//        return ResponseEntity.ok(DataResponseDTO.of("프로젝트 설명 수정이 완료되었습니다."));
//    }

    @PutMapping("/delete/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> deleteProject(@LoginUser Long userId, @PathVariable Long projectId) {

        log.info("요청자 : {}, delete project -> start", userId);
        projectService.deleteProject(userId, projectId);

        log.info("요청자 : {}, delete project -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of("프로젝트가 삭제되었습니다."));
    }

    @PutMapping("/restore/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> restoreProject(@LoginUser Long userId, @PathVariable Long projectId) {

        log.info("요청자 : {}, restore project -> start", userId);
        projectService.restoreProject(userId, projectId);

        log.info("요청자 : {}, restore project -> success", userId);
        return ResponseEntity.ok(DataResponseDTO.of("프로젝트가 복구되었습니다."));
    }

    @MessageMapping("/project/{projectId}/edit/start")
    public void editProjectDescriptionStart(@DestinationVariable Long projectId, Long userId) {
        projectService.startEditProjectDescription(userId, projectId);
    }

    @MessageMapping("/project/{projectId}/edit/update")
    public void broadcastProjectDescriptionUpdate(@DestinationVariable Long projectId, ProjectDescriptionUpdateRequestDTO projectDescriptionUpdateRequestDTO) {
        projectService.broadcastProjectDescriptionUpdate(projectDescriptionUpdateRequestDTO, projectId);
    }

    @PutMapping("/edit/description/{projectId}")
    public ResponseEntity<DataResponseDTO<String>> editProjectDescription(@LoginUser Long userId,
                                                        @PathVariable Long projectId,
                                                        @RequestParam String description) {

        log.info("요청자 : {}, edit project description -> start", userId);
        projectService.editProjectDescription(userId, projectId, description);
        log.info("요청자 : {}, edit project description -> success", userId);

        return ResponseEntity.ok(DataResponseDTO.of("프로젝트 설명 수정이 완료되었습니다."));
    }
}