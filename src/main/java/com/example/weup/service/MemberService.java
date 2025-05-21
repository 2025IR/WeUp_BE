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
    public String inviteUser(Long userId, Long projectId, String email) {
        try {
            User inviter = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            if (!hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            email = email.trim();
            if (email.isEmpty()) {
                throw new GeneralException(ErrorInfo.BAD_REQUEST);
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

                mailService.sendProjectInviteEmail(
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
            e.printStackTrace();
            log.error("사용자 초대 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }



    @Transactional
    public List<MemberInfoResponseDTO> getProjectMembers(Long userId, Long projectId) {
        try {
            log.error(String.valueOf(userId));
            log.error(String.valueOf(projectId));

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
                                .memberId(member.getMemberId())
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

    @Transactional
    public void assignRoleToMember(Long userId, Long projectId, Long memberId, List<String> roleNames) {
        try {
            if (!hasAccess(userId, projectId) || isDeletedMember(memberId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            memberRoleRepository.deleteByMember(member);

            List<Role> roles = roleRepository.findByProjectAndRoleNameIn(project, roleNames);

            if (roles.size() != roleNames.size()) {
                throw new GeneralException(ErrorInfo.ROLE_NOT_FOUND);
            }

            for (Role role : roles) {
                log.debug("roles Data Print : {},{}", role.getRoleName(), role.getRoleId());

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
    public Map<String, Object> createRole(Long userId, Long projectId, String roleName, String roleColor) {
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

            roleRepository.save(role);

            Map<String, Object> result = new HashMap<>();
            result.put("projectId", projectId);
            result.put("roleName", roleName);

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
            if (!hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            if ((roleName == null || roleName.isEmpty()) && (roleColor == null || roleColor.isEmpty())) {
                throw new GeneralException(ErrorInfo.BAD_REQUEST);
            }

            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.BAD_REQUEST));

            role.setRoleName(roleName);
            role.setRoleColor(roleColor);

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
    public void removeRole(Long userId, Long projectId, Long roleId) {
        try {
            if (!hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

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