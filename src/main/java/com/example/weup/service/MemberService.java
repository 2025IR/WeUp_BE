package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.MemberInfoResponseDTO;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final MailService mailService;
    private final ProjectService projectService;
    private final RoleRepository roleRepository;
    private final MemberRoleRepository memberRoleRepository;

    @Transactional
    public Map<String, Object> inviteUsers(Long inviterId, Long projectId, String emailList) {
        try {
            User inviter = userRepository.findById(inviterId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));

            if (!projectService.hasAccess(inviterId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            String[] emails = emailList.split(",");
            List<String> invitedEmails = new ArrayList<>();
            List<String> notFoundEmails = new ArrayList<>();
            List<String> alreadyMemberEmails = new ArrayList<>();
            List<String> withdrawnEmails = new ArrayList<>();

            List<Member> newMembers = new ArrayList<>();

            for (String email : emails) {
                email = email.trim();
                if (email.isEmpty()) continue;

                Optional<User> userOpt = userRepository.findByAccountSocialEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    if (user.isUserWithdrawal()) {
                        withdrawnEmails.add(email);
                        continue;
                    }

                    if (memberRepository.existsByUserAndProject(user, project)) {
                        alreadyMemberEmails.add(email);
                        continue;
                    }

                    mailService.sendProjectInviteEmail(
                            email,
                            user.getName(),
                            inviter.getName(),
                            project.getName()
                    );

                    Member member = new Member();
                    member.setUser(user);
                    member.setProject(project);
                    member.setRole("MEMBER");

                    newMembers.add(member);
                    invitedEmails.add(email);
                } else {
                    notFoundEmails.add(email);
                }
            }

            if (!newMembers.isEmpty()) {
                memberRepository.saveAll(newMembers);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("invitedEmails", invitedEmails);
            result.put("notFoundEmails", notFoundEmails);
            result.put("alreadyMemberEmails", alreadyMemberEmails);
            result.put("withdrawnEmails", withdrawnEmails);

            return result;
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
            if (!projectService.hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            /**
             * memberRoles를 스트림으로 돌려서
             * memberId - roleName 매핑
             *
             * member를 스트림으로 돌려서 user 정보 가져오고
             * 멤버랑 일치하는 역할 부여 [List<String> roles]
             */
            List<Member> members = memberRepository.findByProject_ProjectIdAndIsMemberDeletedFalse(projectId);
            List<Member_Role> memberRoles = memberRoleRepository.findAllByProjectId(projectId);

            Map<Long, List<String>> memberIdToRoleNames = memberRoles.stream()
                    .collect(Collectors.groupingBy(
                            mr -> mr.getMember().getMemberId(),
                            Collectors.mapping(mr -> mr.getRole().getRoleName(), Collectors.toList())
                    ));

            return members.stream()
                    .map(member -> {
                        User user = member.getUser();
                        List<String> roles = memberIdToRoleNames.getOrDefault(member.getMemberId(), new ArrayList<>());

                        return MemberInfoResponseDTO.builder()
                                .userId(user.getUserId())
                                .name(user.getName())
                                .email(user.getAccountSocial().getEmail())
                                .profileImage(user.getProfileImage())
                                .phoneNumber(user.getPhoneNumber())
                                .role(member.getRole())
                                .roles(roles)
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
    public Map<String, Object> delegateLeader(Long formerLeaderUserId, Long projectId, Long newLeaderMemberId) {
        try {
            if (!projectService.hasAccess(formerLeaderUserId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));

            User formerLeaderUser = userRepository.findById(formerLeaderUserId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Member newLeaderMember = memberRepository.findById(newLeaderMemberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Member formerLeaderMember = memberRepository.findByUserAndProject(formerLeaderUser, project)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            if (!"LEADER".equals(formerLeaderMember.getRole())) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            if (!formerLeaderMember.getProject().getProjectId().equals(newLeaderMember.getProject().getProjectId())) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            if (projectService.isDeletedMember(newLeaderMember.getMemberId())){
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            formerLeaderMember.setRole("MEMBER");
            newLeaderMember.setRole("LEADER");

            memberRepository.save(formerLeaderMember);
            memberRepository.save(newLeaderMember);

            Map<String, Object> result = new HashMap<>();
            result.put("projectId", projectId);
            result.put("previousLeaderUserId", formerLeaderUserId);
            result.put("newLeaderMemberId", newLeaderMemberId);
            return result;
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("팀장 위임 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public Map<String, Object> createRole(Long userId, Long projectId, Long memberId, String roleName, String roleColor) {

        try {
            if (!projectService.hasAccess(userId, projectId) || projectService.isDeletedMember(memberId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            Map<String, Object> result = new HashMap<>();

            Optional<Role> existingRole = roleRepository.findByProjectAndRoleName(project, roleName);
            Role role;

            if (existingRole.isPresent()) {
                // 이미 프로젝트에 역할이 존재하는 경우
                role = existingRole.get();

                // 해당 멤버가 이미 그 역할을 갖고 있을 시 에러
                if (memberRoleRepository.existsByMemberAndRole(member, role)) {
                    throw new GeneralException(ErrorInfo.ROLE_ALREADY_GIVEN);
                }

                // 멤버가 역할을 갖고 있지 않으면 연결만 해준다.
                Member_Role memberRole = new Member_Role();
                memberRole.setMember(member);
                memberRole.setRole(role);
                memberRoleRepository.save(memberRole);

            } else {
                // 프로젝트에 역할이 없으면 생성 후 연결
                role = Role.builder()
                        .project(project)
                        .roleName(roleName)
                        .roleColor(roleColor)
                        .build();

                role = roleRepository.save(role);

                Member_Role memberRole = new Member_Role();
                memberRole.setMember(member);
                memberRole.setRole(role);
                memberRoleRepository.save(memberRole);
            }

            result.put("projectId", projectId);
            result.put("memberId", memberId);
            result.put("roleName", roleName);
            result.put("roleColor", role.getRoleColor());

            return result;

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 생성 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }


    @Transactional
    public Map<String, Object> editRole(Long userId, Long projectId, Long roleId, String roleName, String roleColor) {
        try {
            if (!projectService.hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.BAD_REQUEST));

            if (roleName != null && !roleName.isEmpty()) {
                role.setRoleName(roleName);
            }

            if (roleColor != null && !roleColor.isEmpty()) {
                role.setRoleColor(roleColor);
            }

            // 수정된 role 저장
            roleRepository.save(role);

            Map<String, Object> result = new HashMap<>();
            result.put("projectId", projectId);
            result.put("roleId", roleId);
            result.put("roleName", role.getRoleName());
            result.put("roleColor", role.getRoleColor());

            return result;

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 수정 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }


    @Transactional
    public void deleteMember(Long userId, Long projectId, Long memberId) {
        try {
            if (!projectService.hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }
            User requestUser = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));

            Member requestMember = memberRepository.findByUserAndProject(requestUser, project)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Member targetMember = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            if ("LEADER".equals(targetMember.getRole())) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }
            // 삭제 대상이 리더일 경우 불가

            if (!"LEADER".equals(requestMember.getRole()) && !requestMember.getMemberId().equals(memberId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }
            //요청자가 리더거나 본인이거나.

            targetMember.setMemberDeleted(true);
            memberRepository.save(targetMember);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로젝트에서 탈퇴 처리 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    public Map<String, Object> deleteRole(Long userId, Long projectId, Long memberId, String roleName) {
        try {
            if (!projectService.hasAccess(userId, projectId) || projectService.isDeletedMember(memberId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            // 대상자 확인
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            Member_Role memberRole = memberRoleRepository.findByMemberAndRole(member, role)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            memberRoleRepository.delete(memberRole);


            Map<String, Object> result = new HashMap<>();

            result.put("projectId", projectId);
            result.put("memberId", memberId);
            result.put("roleName", roleName);

            return result;

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로젝트에서 탈퇴 처리 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public Map<String, Object> removeRole(Long userId, Long projectId, Long memberId, String roleName) {
        // 먼저 역할 매핑 삭제
        Map<String, Object> result = deleteRole(userId, projectId, memberId, roleName);

        try {
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            memberRoleRepository.deleteByRole(role);

            roleRepository.delete(role);
            result.put("roleRemoved", true);

            return result;

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 완전 삭제 처리 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }
}