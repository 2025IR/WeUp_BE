package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.*;
import com.example.weup.dto.response.MemberInfoResponseDTO;
import com.example.weup.dto.response.RoleListResponseDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.ProjectRepository;
import com.example.weup.repository.RoleRepository;
import com.example.weup.repository.UserRepository;
import com.example.weup.repository.MemberRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final UserRepository userRepository;

    private final ProjectRepository projectRepository;

    private final MemberRepository memberRepository;

    private final AsyncMailService asyncMailService;

    private final S3Service s3Service;

    private final RoleRepository roleRepository;

    private final MemberRoleRepository memberRoleRepository;

    @Transactional
    public void addProjectCreater(Long userId, Project project) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        Member newMember = Member.builder()
                .user(user)
                .project(project)
                .isLeader(true)
                .lastAccessTime(LocalDateTime.now())
                .build();

        project.getMembers().add(newMember);
        memberRepository.save(newMember);
        log.info("create project leader -> db save success : member {}", newMember.getMemberId());
    }

    @Transactional
    public String inviteUser(Long userId, ProjectInviteRequestDTO projectInviteRequestDTO) {
        try {

            User inviter = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(projectInviteRequestDTO.getProjectId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            if (!hasAccess(userId, projectInviteRequestDTO.getProjectId())) {
                throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
            }

            String email = projectInviteRequestDTO.getEmail().trim();
            if (email.isEmpty()) {
                throw new GeneralException(ErrorInfo.EMPTY_INPUT_VALUE);
            }

            Optional<User> userOpt = userRepository.findByAccountSocialEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (user.isUserWithdrawal()) {
                    return "계정이 존재하지 않습니다.";
                }

                if (memberRepository.existsByUserAndProject(user, project)) {
                    return "이미 초대된 계정입니다.";
                }

                asyncMailService.sendProjectInviteEmail(
                        email,
                        user.getName(),
                        inviter.getName(),
                        project.getProjectName()
                );

                Member member = Member.builder()
                        .user(user)
                        .project(project)
                        .isLeader(false)
                        .lastAccessTime(LocalDateTime.now())
                        .build();


                memberRepository.save(member);

                return "초대가 완료되었습니다.";
            } else {
                return "계정이 존재하지 않습니다.";
            }
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 초대 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public List<MemberInfoResponseDTO> getProjectMembers(Long userId, Long projectId) {
        try {
            if (!hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
            }

            List<Member> members = memberRepository.findByProject_ProjectIdAndIsMemberDeletedFalse(projectId);
            List<MemberRole> memberRoles = memberRoleRepository.findAllByProjectId(projectId);

            Map<Long, List<Long>> memberIdToRoleIds = memberRoles.stream()
                    .collect(Collectors.groupingBy(
                            mr -> mr.getMember().getMemberId(),
                            Collectors.mapping(
                                    mr -> mr.getRole().getRoleId(),
                                    Collectors.toList()
                            )
                    ));

            return members.stream()
                    .map(member -> {
                        User user = member.getUser();
                        List<Long> roleIds = memberIdToRoleIds.getOrDefault(member.getMemberId(), new ArrayList<>());

                        return MemberInfoResponseDTO.builder()
                                .memberId(member.getMemberId())
                                .name(user.getName())
                                .email(user.getAccountSocial().getEmail())
                                .profileImage(s3Service.getPresignedUrl(user.getProfileImage()))
                                .phoneNumber(user.getPhoneNumber())
                                .isLeader(member.isLeader())
                                .roleIds(roleIds)
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로젝트 멤버 조회 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }



    @Transactional
    public void delegateLeader(Long formerLeaderUserId, LeaderDelegateRequestDTO leaderDelegateRequestDTO) {
        try {
            if (!hasAccess(formerLeaderUserId, leaderDelegateRequestDTO.getProjectId())) {
                throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
            }

            Project project = projectRepository.findById(leaderDelegateRequestDTO.getProjectId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            User formerLeaderUser = userRepository.findById(formerLeaderUserId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Member newLeaderMember = memberRepository.findById(leaderDelegateRequestDTO.getNewLeaderId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

            Member formerLeaderMember = memberRepository.findByUserAndProject(formerLeaderUser, project)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

            if (!formerLeaderMember.isLeader()) {
                throw new GeneralException(ErrorInfo.NOT_LEADER);
            }

            if (!formerLeaderMember.getProject().getProjectId().equals(newLeaderMember.getProject().getProjectId())) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            if (isDeletedMember(newLeaderMember.getMemberId())){
                throw new GeneralException(ErrorInfo.DELETED_MEMBER);
            }

            formerLeaderMember.demoteFromLeader();
            newLeaderMember.promoteToLeader();

            memberRepository.save(formerLeaderMember);
            memberRepository.save(newLeaderMember);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("팀장 위임 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public List<RoleListResponseDTO> listRoles(Long userId, Long projectId) {
        try {
            if (!hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            List<Role> roles = roleRepository.findAllByProject(project);

            return roles.stream()
                    .map(role -> new RoleListResponseDTO(role.getRoleId(), role.getRoleName(), role.getRoleColor()))
                    .collect(Collectors.toList());

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 조회 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public void deleteMember(Long userId, DeleteMemberRequestDTO deleteMemberRequestDTO) {
        try {
            if (!hasAccess(userId, deleteMemberRequestDTO.getProjectId())) {
                throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
            }
            User requestUser = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(deleteMemberRequestDTO.getProjectId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            Member requestMember = memberRepository.findByUserAndProject(requestUser, project)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

            Member targetMember = memberRepository.findById(deleteMemberRequestDTO.getMemberId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

            if (targetMember.isLeader()) {
                throw new GeneralException(ErrorInfo.NOT_LEADER);
            }

            if (!requestMember.isLeader() && !requestMember.getMemberId().equals(deleteMemberRequestDTO.getMemberId())) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            targetMember.markAsDeleted();
            memberRepository.save(targetMember);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로젝트에서 탈퇴 처리 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public void assignRoleToMember(Long userId, AssignRoleRequestDTO assignRoleRequestDTO) {
        try {
            if (!hasAccess(userId, assignRoleRequestDTO.getProjectId())) {
                throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
            }

            if (isDeletedMember(assignRoleRequestDTO.getMemberId())){
                throw new GeneralException(ErrorInfo.DELETED_MEMBER);
            }

            Project project = projectRepository.findById(assignRoleRequestDTO.getProjectId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            Member member = memberRepository.findById(assignRoleRequestDTO.getMemberId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND));

            memberRoleRepository.deleteByMember(member);
            List<MemberRole> deleteRoles = memberRoleRepository.findByMember(member);
            for (MemberRole role : deleteRoles) {
                log.debug("after deleted data : " + role.getRole().getRoleId() + " : " + role.getRole().getRoleName());
            }

            List<Role> roles = roleRepository.findByProjectAndRoleIdIn(project, assignRoleRequestDTO.getRoleIds());

            if (roles.size() != assignRoleRequestDTO.getRoleIds().size()) {
                throw new GeneralException(ErrorInfo.ROLE_NOT_FOUND);
            }

            for (Role role : roles) {
                MemberRole memberRole = MemberRole.builder()
                        .member(member)
                        .role(role)
                        .build();

                memberRoleRepository.save(memberRole);
            }

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 갱신 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public void createRole(Long userId, CreateRoleRequestDTO createRoleRequestDTO) {
        try {
            if (!hasAccess(userId, createRoleRequestDTO.getProjectId())) {
                throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
            }

            Project project = projectRepository.findById(createRoleRequestDTO.getProjectId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            if (roleRepository.findByProjectAndRoleName(project, createRoleRequestDTO.getRoleName()).isPresent()) {
                throw new GeneralException(ErrorInfo.ROLE_ALREADY_EXISTS);
            }

            Role role = Role.builder()
                    .project(project)
                    .roleName(createRoleRequestDTO.getRoleName())
                    .build();

            roleRepository.save(role);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 생성 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public void editRole(Long userId, EditRoleRequestDTO editRoleRequestDTO) {
        try {
            String roleName = editRoleRequestDTO.getRoleName();
            String roleColor = editRoleRequestDTO.getRoleColor();

            if (!hasAccess(userId, editRoleRequestDTO.getProjectId())) {
                throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
            }

            if ((roleName == null || roleName.isEmpty()) && (roleColor == null || roleColor.isEmpty())) {
                throw new GeneralException(ErrorInfo.EMPTY_INPUT_VALUE);
            }

            Role role = roleRepository.findById(editRoleRequestDTO.getRoleId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.ROLE_NOT_FOUND));

            if (roleName.equals(role.getRoleName()) && roleColor.equals(role.getRoleColor())) {
                return;
            }

            role.editRole(roleName, roleColor);

            roleRepository.save(role);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 수정 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }


    @Transactional
    public void removeRole(Long userId, DeleteRoleRequestDTO deleteRoleRequestDTO) {
        try {
            if (!hasAccess(userId, deleteRoleRequestDTO.getProjectId())) {
                throw new GeneralException(ErrorInfo.NOT_IN_PROJECT);
            }

            Role role = roleRepository.findById(deleteRoleRequestDTO.getRoleId())
                    .orElseThrow(() -> new GeneralException(ErrorInfo.ROLE_NOT_FOUND));

            memberRoleRepository.deleteByRole(role);

            roleRepository.delete(role);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 완전 삭제 처리 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    public boolean hasAccess(Long userId, Long projectId) {
        return memberRepository.existsByUser_UserIdAndProject_ProjectId(userId, projectId);
    }

    public boolean isDeletedMember(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElse(null);

        if (member == null) {
            return true;
        }

        try {
            if (member.isMemberDeleted())
                return true;
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}