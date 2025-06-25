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

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    private final JwtUtil jwtUtil;

    @PostMapping("/invite")
    public ResponseEntity<DataResponseDTO<String>> inviteUser(HttpServletRequest request,
                                                              @RequestBody ProjectInviteRequestDTO projectInviteRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        String result = memberService.inviteUser(userId, projectInviteRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of(result));
    }

    @PostMapping("/list/{projectId}")
    public ResponseEntity<DataResponseDTO<List<MemberInfoResponseDTO>>> getProjectMembers(HttpServletRequest request,
                                                                                          @PathVariable Long projectId) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        List<MemberInfoResponseDTO> members = memberService.getProjectMembers(userId, projectId);
        
        return ResponseEntity.ok(DataResponseDTO.of(members, "프로젝트 멤버 목록 조회 완료"));
    }

    @PutMapping("/delegate/leader")
    public ResponseEntity<DataResponseDTO<String>> delegateLeader(HttpServletRequest request,
                                                                  @RequestBody LeaderDelegateRequestDTO leaderDelegateRequestDTO) {
        
        String token = jwtUtil.resolveToken(request);
        Long formerLeaderUserId = jwtUtil.getUserId(token);
        
        memberService.delegateLeader(formerLeaderUserId, leaderDelegateRequestDTO);
        
        return ResponseEntity.ok(DataResponseDTO.of("팀장 위임이 완료되었습니다."));
    }

    @PutMapping("/delete")
    public ResponseEntity<DataResponseDTO<String>> deleteMember(HttpServletRequest request,
                                                                @RequestBody DeleteMemberRequestDTO deleteMemberRequestDTO){

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        memberService.deleteMember(userId, deleteMemberRequestDTO.getProjectId(), deleteMemberRequestDTO.getMemberId());

        return ResponseEntity.ok(DataResponseDTO.of("프로젝트 탈퇴 처리가 정상적으로 완료되었습니다."));
    }

    @PostMapping("/role/list/{projectId}")
    public ResponseEntity<DataResponseDTO<List<RoleListResponseDTO>>> listRoles(HttpServletRequest request,
                                                                                @PathVariable Long projectId) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        List<RoleListResponseDTO> roleList = memberService.listRoles(userId, projectId);

        return ResponseEntity.ok(DataResponseDTO.of(roleList, "역할 목록 조회가 완료되었습니다."));
    }

    @PutMapping("/role/assign")
    public ResponseEntity<DataResponseDTO<String>> assignRoleToMember(HttpServletRequest request,
                                                                      @RequestBody AssignRoleRequestDTO assignRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        memberService.assignRoleToMember(userId, assignRoleRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("역할이 추가되었습니다"));
    }

    @PostMapping("/role/create")
    public ResponseEntity<DataResponseDTO<String>> createRole(HttpServletRequest request,
                                                              @RequestBody CreateRoleRequestDTO createRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        memberService.createRole(userId, createRoleRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("역할이 추가되었습니다."));
    }

    @PutMapping("/role/edit")
    public ResponseEntity<DataResponseDTO<String>> editRole(HttpServletRequest request,
                                                            @RequestBody EditRoleRequestDTO editRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        memberService.editRole(userId, editRoleRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("역할이 수정되었습니다."));
    }

    @DeleteMapping("/role/remove")
    public ResponseEntity<DataResponseDTO<String>> removeRole(HttpServletRequest request,
                                                              @RequestBody DeleteRoleRequestDTO deleteRoleRequestDTO) {

        String token = jwtUtil.resolveToken(request);
        Long userId = jwtUtil.getUserId(token);

        memberService.removeRole(userId, deleteRoleRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("역할이 삭제되었습니다."));
    }
}