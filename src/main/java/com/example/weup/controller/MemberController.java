package com.example.weup.controller;

import com.example.weup.HandlerMethodArgumentResolver.annotation.LoginUser;
import com.example.weup.dto.request.*;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.MemberInfoResponseDTO;
import com.example.weup.dto.response.RoleListResponseDTO;
import com.example.weup.service.MemberService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/invite")
    public ResponseEntity<DataResponseDTO<String>> inviteUser(@LoginUser Long userId,
                                                              @Valid @RequestBody ProjectInviteRequestDTO projectInviteRequestDTO) throws JsonProcessingException {

        String result = memberService.inviteUser(userId, projectInviteRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of(result));
    }

    @PostMapping("/list/{projectId}")
    public ResponseEntity<DataResponseDTO<List<MemberInfoResponseDTO>>> getProjectMembers(@LoginUser Long userId,
                                                                                          @PathVariable Long projectId) {

        List<MemberInfoResponseDTO> members = memberService.getProjectMembers(userId, projectId);

        return ResponseEntity.ok(DataResponseDTO.of(members, "프로젝트 멤버 목록 조회 완료"));
    }

    @PutMapping("/delegate/leader")
    public ResponseEntity<DataResponseDTO<String>> delegateLeader(@LoginUser Long userId,
                                                                  @RequestBody LeaderDelegateRequestDTO leaderDelegateRequestDTO) {

        memberService.delegateLeader(userId, leaderDelegateRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("팀장 위임이 완료되었습니다."));
    }

    @PutMapping("/delete")
    public ResponseEntity<DataResponseDTO<String>> deleteMember(@LoginUser Long userId,
                                                                @RequestBody DeleteMemberRequestDTO deleteMemberRequestDTO) {

        memberService.deleteMember(userId, deleteMemberRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("프로젝트 탈퇴 처리가 정상적으로 완료되었습니다."));
    }

    @PostMapping("/role/list/{projectId}")
    public ResponseEntity<DataResponseDTO<List<RoleListResponseDTO>>> listRoles(@LoginUser Long userId,
                                                                                @PathVariable Long projectId) {

        List<RoleListResponseDTO> roleList = memberService.listRoles(userId, projectId);

        return ResponseEntity.ok(DataResponseDTO.of(roleList, "역할 목록 조회가 완료되었습니다."));
    }

    @PutMapping("/role/assign")
    public ResponseEntity<DataResponseDTO<String>> assignRoleToMember(@LoginUser Long userId,
                                                                      @RequestBody AssignRoleRequestDTO assignRoleRequestDTO) {

        memberService.assignRoleToMember(userId, assignRoleRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("역할이 추가되었습니다"));
    }

    @PostMapping("/role/create")
    public ResponseEntity<DataResponseDTO<String>> createRole(@LoginUser Long userId,
                                                              @RequestBody CreateRoleRequestDTO createRoleRequestDTO) {

        memberService.createRole(userId, createRoleRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("역할이 추가되었습니다."));
    }

    @PutMapping("/role/edit")
    public ResponseEntity<DataResponseDTO<String>> editRole(@LoginUser Long userId,
                                                            @RequestBody EditRoleRequestDTO editRoleRequestDTO) {

        memberService.editRole(userId, editRoleRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("역할이 수정되었습니다."));
    }

    @DeleteMapping("/role/remove")
    public ResponseEntity<DataResponseDTO<String>> removeRole(@LoginUser Long userId,
                                                              @RequestBody DeleteRoleRequestDTO deleteRoleRequestDTO) {

        memberService.removeRole(userId, deleteRoleRequestDTO);

        return ResponseEntity.ok(DataResponseDTO.of("역할이 삭제되었습니다."));
    }
}
