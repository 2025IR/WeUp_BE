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
import com.example.weup.validate.MemberValidator;
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

    private final MemberValidator memberValidator;

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
        User inviter = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        Project project = projectRepository.findById(projectInviteRequestDTO.getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        memberValidator.validateActiveMemberInProject(userId, projectInviteRequestDTO.getProjectId());

        String email = projectInviteRequestDTO.getEmail().trim();
        if (email.isEmpty()) {
            throw new GeneralException(ErrorInfo.EMPTY_INPUT_VALUE);
        }

        Optional<User> userOpt = userRepository.findByAccountSocialEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.isUserWithdrawal()) {
                throw new GeneralException(ErrorInfo.USER_NOT_FOUND);
            }

            Optional<Member> existingMemberOpt = memberRepository.findByUserAndProject(user, project);
            if (existingMemberOpt.isPresent()) {
                Member existingMember = existingMemberOpt.get();

                if (existingMember.isMemberDeleted()) {
                    existingMember.reJoin();
                    return "초대가 완료되었습니다.";
                } else {
                    throw new GeneralException(ErrorInfo.AlREADY_IN_PROJECT);
                }
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
            throw new GeneralException(ErrorInfo.USER_NOT_FOUND);
        }
    }


    @Transactional
    public List<MemberInfoResponseDTO> getProjectMembers(Long userId, Long projectId) {
        memberValidator.validateActiveMemberInProject(userId, projectId);

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
    }

    @Transactional
    public void delegateLeader(Long formerLeaderUserId, LeaderDelegateRequestDTO leaderDelegateRequestDTO) {
        memberValidator.validateActiveMemberInProject(formerLeaderUserId, leaderDelegateRequestDTO.getProjectId());

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

        if (memberValidator.isDeletedMember(newLeaderMember.getMemberId())){
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        formerLeaderMember.demoteFromLeader();
        newLeaderMember.promoteToLeader();

        memberRepository.save(formerLeaderMember);
        memberRepository.save(newLeaderMember);
    }

    @Transactional
    public List<RoleListResponseDTO> listRoles(Long userId, Long projectId) {
        memberValidator.validateActiveMemberInProject(userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        List<Role> roles = roleRepository.findAllByProject(project);

        return roles.stream()
                .map(role -> new RoleListResponseDTO(role.getRoleId(), role.getRoleName(), role.getRoleColor()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMember(Long userId, DeleteMemberRequestDTO deleteMemberRequestDTO) {
        memberValidator.validateActiveMemberInProject(userId, deleteMemberRequestDTO.getProjectId());

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
    }

    @Transactional
    public void assignRoleToMember(Long userId, AssignRoleRequestDTO assignRoleRequestDTO) {
        memberValidator.validateActiveMemberInProject(userId, assignRoleRequestDTO.getProjectId());

        if (memberValidator.isDeletedMember(assignRoleRequestDTO.getMemberId())){
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
    }

    @Transactional
    public void createRole(Long userId, CreateRoleRequestDTO createRoleRequestDTO) {
        memberValidator.validateActiveMemberInProject(userId, createRoleRequestDTO.getProjectId());

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
    }

    @Transactional
    public void editRole(Long userId, EditRoleRequestDTO editRoleRequestDTO) {
        String roleName = editRoleRequestDTO.getRoleName();
        String roleColor = editRoleRequestDTO.getRoleColor();

        memberValidator.validateActiveMemberInProject(userId, editRoleRequestDTO.getProjectId());

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
    }

    @Transactional
    public void removeRole(Long userId, DeleteRoleRequestDTO deleteRoleRequestDTO) {
        memberValidator.validateActiveMemberInProject(userId, deleteRoleRequestDTO.getProjectId());


        Role role = roleRepository.findById(deleteRoleRequestDTO.getRoleId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.ROLE_NOT_FOUND));

        memberRoleRepository.deleteByRole(role);

        roleRepository.delete(role);
    }

}