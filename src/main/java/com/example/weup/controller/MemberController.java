package com.example.weup.controller;

import com.example.weup.dto.request.*;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.MemberInfoResponseDTO;
import com.example.weup.security.JwtUtil;
import com.example.weup.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    @PostMapping("/invite")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> inviteUsers(
            HttpServletRequest request,
            @RequestBody ProjectInviteRequestDTO projectInviteRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = memberService.inviteUsers(
                userId,
                projectInviteRequestDTO.getProjectId(),
                projectInviteRequestDTO.getEmails()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "초대 처리가 완료되었습니다."));
    }

    @PostMapping("/join/{projectId}")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> joinProject(
            HttpServletRequest request,
            @PathVariable Long projectId) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = memberService.joinProject(userId, projectId);

        return ResponseEntity.ok(DataResponseDTO.of(result, "프로젝트 참여가 완료되었습니다."));
    }

    @PostMapping("/list")
    public ResponseEntity<DataResponseDTO<List<MemberInfoResponseDTO>>> getProjectMembers(
            HttpServletRequest request,
            @RequestBody ProjectMemberRequestDTO projectMemberRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        List<MemberInfoResponseDTO> members = memberService.getProjectMembers(
                userId, 
                projectMemberRequestDTO.getProjectId()
        );
        
        return ResponseEntity.ok(DataResponseDTO.of(members, "프로젝트 멤버 목록 조회 완료"));
    }

    @PutMapping("/delegate/leader")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> delegateLeader(
            HttpServletRequest request,
            @RequestBody LeaderDelegateRequestDTO leaderDelegateRequestDTO) {
        
        String token = jwtUtil.resolveToken(request);
        Long formerLeaderId = jwtUtil.getUserId(token);
        
        Map<String, Object> result = memberService.delegateLeader(
                formerLeaderId,
                leaderDelegateRequestDTO.getProjectId(),
                leaderDelegateRequestDTO.getNewLeaderId()
        );
        
        return ResponseEntity.ok(DataResponseDTO.of(result, "팀장 위임이 완료되었습니다."));
    }

    @PostMapping("role/edit")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> editRole(
            HttpServletRequest request,
            @RequestBody EditRoleRequestDTO editRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = memberService.editRole(
                userId,
                editRoleRequestDTO.getProjectId(),
                editRoleRequestDTO.getMemberId(),
                editRoleRequestDTO.getRoleName(),
                editRoleRequestDTO.getRoleColor()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "역할이 추가되었습니다."));
    }

    @PutMapping("/delete")
    public ResponseEntity<DataResponseDTO<String>> deleteMember(
            HttpServletRequest request,
            @RequestBody DeleteMemberRequestDTO deleteMemberRequestDTO){

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        memberService.deleteMember(
                userId,
                deleteMemberRequestDTO.getProjectId(),
                deleteMemberRequestDTO.getMemberId()
        );

        String message = "프로젝트 탈퇴 처리가 정상적으로 완료되었습니다.";

        return ResponseEntity.ok(DataResponseDTO.of(message));

    }

    @PutMapping("role/delete")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> deleteRole(
            HttpServletRequest request,
            @RequestBody DeleteRoleRequestDTO deleteRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = memberService.deleteRole(
                userId,
                deleteRoleRequestDTO.getProjectId(),
                deleteRoleRequestDTO.getMemberId(),
                deleteRoleRequestDTO.getRoleName()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "역할이 해제되었습니다."));
    }

    @DeleteMapping("info/role/remove")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> removeRole(
            HttpServletRequest request,
            @RequestBody DeleteRoleRequestDTO deleteRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = memberService.removeRole(
                userId,
                deleteRoleRequestDTO.getProjectId(),
                deleteRoleRequestDTO.getMemberId(),
                deleteRoleRequestDTO.getRoleName()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "역할이 삭제되었습니다."));
    }
}