package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.constant.NotificationType;
import com.example.weup.dto.request.*;
import com.example.weup.dto.response.MemberInfoResponseDTO;
import com.example.weup.dto.response.RoleListResponseDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.*;
import com.example.weup.validate.MemberValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final AsyncMailService asyncMailService;
    private final S3Service s3Service;
    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final NotificationService notificationService;
    private final MemberValidator memberValidator;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Member addProjectCreater(Long userId, Project project) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        Member newMember = Member.builder()
                .user(user)
                .project(project)
                .isLeader(true)
                .lastAccessTime(LocalDateTime.now())
                .build();

        project.getMembers().add(newMember);
        log.info("create project leader -> db save success : member {}", newMember.getMemberId());
        return memberRepository.save(newMember);

    }

    @Transactional
    public String inviteUser(Long userId, ProjectInviteRequestDTO projectInviteRequestDTO) throws JsonProcessingException {
        User inviter = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        Project project = projectRepository.findById(projectInviteRequestDTO.getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        memberValidator.validateActiveMemberInProject(userId, project.getProjectId());

        String email = projectInviteRequestDTO.getEmail().trim();
        User invitee = userRepository.findByAccountSocialEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        if (invitee.isUserWithdrawal()) throw new GeneralException(ErrorInfo.USER_NOT_FOUND);

        Member existingMember = memberRepository.findByUserAndProject(invitee, project).orElse(null);

        if (existingMember != null) {
            if (existingMember.isMemberDeleted()) {
                existingMember.reJoin();
                chatRoomService.addChatRoomMember(chatRoomRepository.findByProjectAndBasicTrue(project), existingMember.getMemberId());
              
                String msg = NotificationType.MEMBER_INVITED.format(invitee.getName(), project.getProjectName());
                notificationService.sendPersonalNotification(invitee, msg, "INVITE", projectInviteRequestDTO.getProjectId());
                notificationService.broadcastProjectNotification(project, msg, List.of(invitee.getUserId()), "INVITE");

                messagingTemplate.convertAndSend(
                        "/topic/member/" + projectInviteRequestDTO.getProjectId(),
                        Map.of("type", "LIST_CHANGED",
                                "editedBy", inviter.getName())
                );

                return "초대가 완료되었습니다.";
            } else {
                throw new GeneralException(ErrorInfo.ALREADY_IN_PROJECT);
            }
        }

        asyncMailService.sendProjectInviteEmail(email, invitee.getName(), inviter.getName(), project.getProjectName());

        Member member = Member.builder()
                .user(invitee)
                .project(project)
                .isLeader(false)
                .lastAccessTime(LocalDateTime.now())
                .build();
        memberRepository.save(member);

        chatRoomService.addChatRoomMember(chatRoomRepository.findByProjectAndBasicTrue(project), member.getMemberId());

        String msg = NotificationType.MEMBER_INVITED.format(invitee.getName(), project.getProjectName());
        notificationService.sendPersonalNotification(invitee, msg, "INVITE", projectInviteRequestDTO.getProjectId());
        notificationService.broadcastProjectNotification(project, msg, List.of(invitee.getUserId()), "INVITE");

        messagingTemplate.convertAndSend(
                "/topic/member/" + projectInviteRequestDTO.getProjectId(),
                Map.of("type", "LIST_CHANGED",
                        "editedBy", inviter.getName())
        );

        return "초대가 완료되었습니다.";
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

        messagingTemplate.convertAndSend(
                "/topic/member/" + leaderDelegateRequestDTO.getProjectId(),
                Map.of("type", "LIST_CHANGED",
                        "editedBy", formerLeaderMember.getUser().getName())
        );

        notificationService.sendPersonalNotification(newLeaderMember.getUser(),
                NotificationType.LEADER_DELEGATED.format(project.getProjectName(),newLeaderMember.getUser().getName()),
                "DELEGATE", leaderDelegateRequestDTO.getProjectId());
    }

    @Transactional
    public void deleteMember(Long userId, DeleteMemberRequestDTO deleteMemberRequestDTO) throws JsonProcessingException {
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

        List<MemberRole> memberRoleList = memberRoleRepository.findAllByMember_MemberId(targetMember.getMemberId());
        memberRoleRepository.deleteAll(memberRoleList);



        List<ChatRoom> chatRooms = chatRoomRepository.findByProject(project);
        for (ChatRoom chatRoom : chatRooms) {
            ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, targetMember);
            if (chatRoomMember != null) {
                chatRoomMemberRepository.delete(chatRoomMember);
                chatService.sendSystemMessage(chatRoom.getChatRoomId(), chatRoomMember.getMember().getUser().getName() + "님이 채팅방에서 퇴장했습니다.");
            }
        }

        String msg = NotificationType.MEMBER_DELETED.format(targetMember.getUser().getName(), project.getProjectName());
        notificationService.sendPersonalNotification(targetMember.getUser(), msg, "DELETE", deleteMemberRequestDTO.getProjectId());
        notificationService.broadcastProjectNotification(project, msg, List.of(targetMember.getUser().getUserId()), "DELETE");

        messagingTemplate.convertAndSend(
                "/topic/member/" + deleteMemberRequestDTO.getProjectId(),
                Map.of("type", "LIST_CHANGED",
                        "editedBy", requestMember.getUser().getName())
        );
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
    public void assignRoleToMember(Long userId, AssignRoleRequestDTO assignRoleRequestDTO) {
        Member requestMember = memberValidator.validateActiveMemberInProject(userId, assignRoleRequestDTO.getProjectId());

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

            messagingTemplate.convertAndSend(
                    "/topic/member/" + assignRoleRequestDTO.getProjectId(),
                    Map.of("type", "LIST_CHANGED",
                            "editedBy", member.getUser().getName(),
                            "memberId", requestMember.getMemberId())
            );
        }
    }

    @Transactional
    public void createRole(Long userId, CreateRoleRequestDTO createRoleRequestDTO) {
        Member member = memberValidator.validateActiveMemberInProject(userId, createRoleRequestDTO.getProjectId());

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

        messagingTemplate.convertAndSend(
                "/topic/member/" + createRoleRequestDTO.getProjectId(),
                Map.of("type", "ROLE_CHANGED",
                        "editedBy", member.getUser().getName(),
                        "memberId", member.getMemberId())
        );
    }

    @Transactional
    public void editRole(Long userId, EditRoleRequestDTO editRoleRequestDTO) {
        String roleName = editRoleRequestDTO.getRoleName();
        String roleColor = editRoleRequestDTO.getRoleColor();

        Member member = memberValidator.validateActiveMemberInProject(userId, editRoleRequestDTO.getProjectId());

        if ((roleName == null || roleName.isEmpty()) && (roleColor == null || roleColor.isEmpty())) {
            throw new GeneralException(ErrorInfo.EMPTY_INPUT_VALUE);
        }

        Role role = roleRepository.findById(editRoleRequestDTO.getRoleId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.ROLE_NOT_FOUND));

        if (roleName.equals(role.getRoleName()) && roleColor.equals(role.getRoleColor())) {
            return;
        }

        role.editRole(roleName, roleColor);

        messagingTemplate.convertAndSend(
                "/topic/member/" + editRoleRequestDTO.getProjectId(),
                Map.of("type", "ROLE_CHANGED",
                        "editedBy", member.getUser().getName(),
                        "memberId", member.getMemberId())
        );

        roleRepository.save(role);
    }

    @Transactional
    public void removeRole(Long userId, DeleteRoleRequestDTO deleteRoleRequestDTO) {
        Member member = memberValidator.validateActiveMemberInProject(userId, deleteRoleRequestDTO.getProjectId());

        Role role = roleRepository.findById(deleteRoleRequestDTO.getRoleId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.ROLE_NOT_FOUND));

        memberRoleRepository.deleteByRole(role);

        roleRepository.delete(role);

        messagingTemplate.convertAndSend(
                "/topic/member/" + deleteRoleRequestDTO.getProjectId(),
                Map.of("type", "ROLE_CHANGED",
                        "editedBy", member.getUser().getName(),
                        "memberId", member.getMemberId())
        );
    }
}