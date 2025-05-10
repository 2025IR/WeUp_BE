package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
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
    private final MailService mailService;
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
    }

    @Transactional
    public Map<String, Object> inviteUsers(Long inviterId, Long projectId, String emailList) {
        try {
            User inviter = userRepository.findById(inviterId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            if (!hasAccess(inviterId, projectId)) {
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
                            project.getProjectName()
                    );

                    Member member = new Member();
                    member.setUser(user);
                    member.setProject(project);
                    member.setLeader(false);
                    member.setLastAccessTime(LocalDateTime.now());

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
            if (!hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            List<Member> members = memberRepository.findByProject_ProjectIdAndIsMemberDeletedFalse(projectId);
            List<MemberRole> memberRoles = memberRoleRepository.findAllByProjectId(projectId);

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
                                .profileImage(s3Service.getPresignedUrl(user.getProfileImage()))
                                .phoneNumber(user.getPhoneNumber())
                                .isLeader(false)
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
            if (!hasAccess(formerLeaderUserId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            User formerLeaderUser = userRepository.findById(formerLeaderUserId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Member newLeaderMember = memberRepository.findById(newLeaderMemberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Member formerLeaderMember = memberRepository.findByUserAndProject(formerLeaderUser, project)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            if (!formerLeaderMember.isLeader()) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            if (!formerLeaderMember.getProject().getProjectId().equals(newLeaderMember.getProject().getProjectId())) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            if (isDeletedMember(newLeaderMember.getMemberId())){
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            formerLeaderMember.setLeader(false);
            newLeaderMember.setLeader(true);

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
    public List<RoleListResponseDTO> listRoles(Long userId, Long projectId) {
        try {
            if (!hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
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
    public Role createRole(Long userId, Long projectId, String roleName, String roleColor) {
        try {
            if (!hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            if (roleRepository.findByProjectAndRoleName(project, roleName).isPresent()) {
                throw new GeneralException(ErrorInfo.ROLE_ALREADY_EXISTS);
            }

            Role role = Role.builder()
                    .project(project)
                    .roleName(roleName)
                    .roleColor(roleColor)
                    .build();

            return roleRepository.save(role);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 생성 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public Map<String, Object> assignRoleToMember(Long userId, Long projectId, Long memberId, String roleName, String roleColor) {
        try {
            if (!hasAccess(userId, projectId) || isDeletedMember(memberId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            Map<String, Object> result = new HashMap<>();

            Optional<Role> existingRole = roleRepository.findByProjectAndRoleName(project, roleName);
            Role role;

            role = existingRole.orElseGet(() -> createRole(userId, projectId, roleName, roleColor));

            if (memberRoleRepository.existsByMemberAndRole(member, role)) {
                throw new GeneralException(ErrorInfo.ROLE_ALREADY_GIVEN);
            }

            MemberRole memberRole = new MemberRole();
            memberRole.setMember(member);
            memberRole.setRole(role);
            memberRoleRepository.save(memberRole);

            result.put("projectId", projectId);
            result.put("memberId", memberId);
            result.put("roleName", role.getRoleName());
            result.put("roleColor", role.getRoleColor());

            return result;

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 부여 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }



    @Transactional
    public Map<String, Object> editRole(Long userId, Long projectId, Long roleId, String roleName, String roleColor) {
        try {
            if (!hasAccess(userId, projectId)) {
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
            if (!hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }
            User requestUser = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            Member requestMember = memberRepository.findByUserAndProject(requestUser, project)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Member targetMember = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            if (targetMember.isLeader()) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            if (!requestMember.isLeader() && !requestMember.getMemberId().equals(memberId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

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
            if (!hasAccess(userId, projectId) || isDeletedMember(memberId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            MemberRole memberRole = memberRoleRepository.findByMemberAndRole(member, role)
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

    public boolean hasAccess(Long userId, Long projectId) {

        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if ("ROLE_ADMIN".equals(user.getRole())) {
            return true;
        }

        try {
            Project project = projectRepository.findById(projectId)
                    .orElse(null);

            if (project == null) {
                return false;
            }

            try {
                return memberRepository.existsByUserAndProject(user, project);
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
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