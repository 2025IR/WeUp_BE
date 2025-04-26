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
    public Map<String, Object> inviteUsers(Long inviterId, Long projectId, String emailsString) {
        try {
            User inviter = userRepository.findById(inviterId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));

            if (!projectService.hasAccess(inviterId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            String[] emails = emailsString.split(",");
            List<String> invitedEmails = new ArrayList<>();         //초대됨
            List<String> notFoundEmails = new ArrayList<>();        //없음
            List<String> alreadyMemberEmails = new ArrayList<>();   //이미멤버임

            for (String email : emails) {
                email = email.trim();
                if (email.isEmpty()) continue;

                Optional<User> user_Opt = userRepository.findByAccountSocialEmail(email);
                if (user_Opt.isPresent()) {
                    User user = user_Opt.get();

                    // 이미 멤버인지 확인
                    if (memberRepository.existsByUserAndProject(user, project)) {
                        alreadyMemberEmails.add(email);
                        continue;
                    }

                    /** 초대 이메일 발송
                     */
                    mailService.sendProjectInviteEmail(
                            email,
                            user.getName(),
                            inviter.getName(),
                            projectId,
                            project.getName()
                    );

                    invitedEmails.add(email);
                } else {
                    notFoundEmails.add(email);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("invitedEmails", invitedEmails);
            result.put("notFoundEmails", notFoundEmails);
            result.put("alreadyMemberEmails", alreadyMemberEmails);

            return result;
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public Map<String, Object> joinProject(Long userId, Long projectId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));

            if (memberRepository.existsByUserAndProject(user, project)) {
                throw new GeneralException(ErrorInfo.AlREADY_IN_PROJECT);
            }

            Member member = new Member();
            member.setUser(user);
            member.setProject(project);
            member.setRole("MEMBER");

            memberRepository.save(member);
            //todo. 이거 그냥 url로 접근하면 저장됨 -> 보통 이메일 보낼 때 토큰 로직도 구현하는 것 같음
            //todo. /api/verify?token= 이런 느낌으로. 이것도 redis 쓸 때 개선하기?

            Map<String, Object> result = new HashMap<>();
            result.put("projectId", project.getProjectId());
            result.put("projectName", project.getName());
            result.put("userId", user.getUserId());
            result.put("userName", user.getName());
            result.put("memberRole", member.getRole());

            return result;
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로젝트 참여 중 오류 발생", e);
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
            List<Member> members = memberRepository.findByProject_ProjectId(projectId);
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
    public Map<String, Object> delegateLeader(Long formerLeaderId, Long projectId, Long newLeaderId) {
        try {
            if (!projectService.hasAccess(formerLeaderId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));

            User formerLeader = userRepository.findById(formerLeaderId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            User newLeader = userRepository.findById(newLeaderId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Member formerLeaderMember = memberRepository.findByUserAndProject(formerLeader, project)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));
            
            //todo. 이런 예외처리 개선 가능성?
            // 액세스 토큰에 담긴 정보는 유저 거니까, 유저+프로젝트로 멤버를 찾게끔 했는데, 음..

            if (!"LEADER".equals(formerLeaderMember.getRole())) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }

            Member newLeaderMember = memberRepository.findByUserAndProject(newLeader, project)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            formerLeaderMember.setRole("MEMBER");
            newLeaderMember.setRole("LEADER");

            memberRepository.save(formerLeaderMember);
            memberRepository.save(newLeaderMember);

            Map<String, Object> result = new HashMap<>();
            result.put("projectId", projectId);
            result.put("previousLeaderId", formerLeaderId);
            result.put("newLeaderId", newLeaderId);
            return result;
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("팀장 위임 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public Map<String, Object> editRole(Long userId, Long projectId, Long memberId, String roleName, Byte roleColor) {
        /**
         * 매개변수 바탕으로 role에 행 만들고
         * 멤버아이디랑 role 행 연결하고 - memberRole
         *
         * projID memID roleName,Color 반환하고
         *
         * 아이디 있는지 확인하고
         *
         * 입력받은 롤네임이 있으면 멤버롤에만 추가, 없으면 롤에 생성하고 멤버롤에 추가
         *
         */
        try {
            if (!projectService.hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }
            //요청자 확인
//            User requestUser = userRepository.findById(userId)
//                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));
            
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));

//            Member requestMember = memberRepository.findByUserAndProject(requestUser, project)
//                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));
//
//            if (!"LEADER".equals(requestMember.getRole())) {
//                throw new GeneralException(ErrorInfo.FORBIDDEN);
//            }
            /**
             * 위의 주석 처리 부분 - 요청자의 권한이 리더일 경우
             */
            
            // 대상자 확인
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            Map<String, Object> result = new HashMap<>();
            
            // 해당 프로젝트에 그 롤 이름이 있으면
            Role role;
            Optional<Role> existingRole = roleRepository.findByProjectAndRoleName(project, roleName);
            
            if (existingRole.isPresent()) {
                // 갖다쓰고
                role = existingRole.get();
            } else {
                // 없으면 입력값에 기반해 생성
                role = Role.builder()
                        .project(project)
                        .roleName(roleName)
                        .roleColor(roleColor)
                        .build();
                
                role = roleRepository.save(role);
            }
            
            // 멤버가 역할 갖고있는지 확인하고
            if (!memberRoleRepository.existsByMemberAndRole(member, role)) {
                Member_Role memberRole = new Member_Role();
                memberRole.setMember(member);
                memberRole.setRole(role);
                
                // 없으면 저장
                memberRoleRepository.save(memberRole);
            }
            
            // 결과 반환
            result.put("projectId", projectId);
            result.put("memberId", memberId);
            result.put("roleName", roleName);
            result.put("roleColor", roleColor);
            
            return result;
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 생성 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    @Transactional
    public void deleteMember(Long userId, Long projectId, Long memberId) {
        try {
            if (!projectService.hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }
            //요청자 확인
            User requestUser = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));

            Member requestMember = memberRepository.findByUserAndProject(requestUser, project)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            if (!"LEADER".equals(requestMember.getRole()) && !requestMember.getMemberId().equals(memberId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }
            //요청자가 리더거나 본인이거나.

            requestMember.setMemberDeleted(true);

            memberRepository.save(requestMember);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로젝트에서 탈퇴 처리 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

    public Map<String, Object> deleteRole(Long userId, Long projectId, Long memberId, String roleName) {
        try {
            if (!projectService.hasAccess(userId, projectId)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }
            //요청자 확인 - 여기도 요청자가 리더인지 확인하는 부분
//            User requestUser = userRepository.findById(userId)
//                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));
//
//            Project project = projectRepository.findById(projectId)
//                    .orElseThrow(() -> new GeneralException(ErrorInfo.INTERNAL_ERROR));
//
//            Member requestMember = memberRepository.findByUserAndProject(requestUser, project)
//                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));
//
//            if (!"LEADER".equals(requestMember.getRole())) {
//                throw new GeneralException(ErrorInfo.FORBIDDEN);
//            }

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
            // roleName으로 Role 다시 조회
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

            // 프로젝트의 타 멤버가 역할 안 쓰는거 맞는지 확인하고 삭제
            boolean isRoleStillUsed = memberRoleRepository.existsByRole(role);
            if (!isRoleStillUsed) {
                roleRepository.delete(role);
                result.put("roleRemoved", true);
            } else {
                result.put("roleRemoved", false);
            }

            return result;

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("역할 완전 삭제 처리 중 오류 발생", e);
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }

}