package com.example.weup.controller;

import com.example.weup.dto.request.*;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.MemberInfoResponseDTO;
import com.example.weup.dto.response.RoleListResponseDTO;
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

        System.out.println(">>> projectId: " + projectInviteRequestDTO.getProjectId());
        System.out.println(">>> email: " + projectInviteRequestDTO.getEmail());

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        String result = memberService.inviteUser(
                userId,
                projectInviteRequestDTO.getProjectId(),
                projectInviteRequestDTO.getEmail()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result));
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
        Long formerLeaderUserId = jwtUtil.getUserId(token);
        
        Map<String, Object> result = memberService.delegateLeader(
                formerLeaderUserId,
                leaderDelegateRequestDTO.getProjectId(),
                leaderDelegateRequestDTO.getNewLeaderId()
        );
        
        return ResponseEntity.ok(DataResponseDTO.of(result, "팀장 위임이 완료되었습니다."));
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

    @PostMapping("/role/list")
    public ResponseEntity<DataResponseDTO<List<RoleListResponseDTO>>> listRoles(
            HttpServletRequest request,
            @RequestBody ListRoleRequestDTO listRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        List<RoleListResponseDTO> roleList = memberService.listRoles(
                userId,
                listRoleRequestDTO.getProjectId()
        );

        return ResponseEntity.ok(DataResponseDTO.of(roleList, "역할 목록 조회가 완료되었습니다."));
    }

    @PutMapping("/role/assign")
    public ResponseEntity<String> assignRoleToMember(
            HttpServletRequest request,
            @RequestBody AssignRoleRequestDTO assignRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        memberService.assignRoleToMember(
                userId,
                assignRoleRequestDTO.getProjectId(),
                assignRoleRequestDTO.getMemberId(),
                assignRoleRequestDTO.getRoleName()
                );

        return ResponseEntity.ok("역할이 추가되었습니다.");  // Response로 바꿔야 함
    }

    @PostMapping("/role/create")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> createRole(
            HttpServletRequest request,
            @RequestBody CreateRoleRequestDTO createRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = memberService.createRole(
                userId,
                createRoleRequestDTO.getProjectId(),
                createRoleRequestDTO.getRoleName(),
                createRoleRequestDTO.getRoleColor()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "역할이 추가되었습니다."));
    }

    @PutMapping("/role/edit")
    public ResponseEntity<DataResponseDTO<Map<String, Object>>> editRole(
            HttpServletRequest request,
            @RequestBody EditRoleRequestDTO editRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        Map<String, Object> result = memberService.editRole(
                userId,
                editRoleRequestDTO.getProjectId(),
                editRoleRequestDTO.getRoleId(),
                editRoleRequestDTO.getRoleName(),
                editRoleRequestDTO.getRoleColor()
        );

        return ResponseEntity.ok(DataResponseDTO.of(result, "역할이 수정되었습니다."));
    }

//    @PutMapping("/role/delete")
//    public ResponseEntity<DataResponseDTO<Map<String, Object>>> deleteRole(
//            HttpServletRequest request,
//            @RequestBody DeleteRoleRequestDTO deleteRoleRequestDTO) {
//
//        String token = jwtUtil.resolveToken(request);
//        Long userId = jwtUtil.getUserId(token);
//
//        Map<String, Object> result = memberService.deleteRole(
//                userId,
//                deleteRoleRequestDTO.getProjectId(),
//                deleteRoleRequestDTO.getMemberId(),
//                deleteRoleRequestDTO.getRoleName()
//        );
//
//        return ResponseEntity.ok(DataResponseDTO.of(result, "역할이 해제되었습니다."));
//    }

    @DeleteMapping("/role/remove")
    public ResponseEntity<DataResponseDTO<String>> removeRole(
            HttpServletRequest request,
            @RequestBody DeleteRoleRequestDTO deleteRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        memberService.removeRole(
                userId,
                deleteRoleRequestDTO.getProjectId(),
                deleteRoleRequestDTO.getRoleId()
        );

        return ResponseEntity.ok(DataResponseDTO.of("역할이 삭제되었습니다."));
    }
}