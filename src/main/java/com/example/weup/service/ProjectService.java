package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.entity.Member;
import com.example.weup.entity.Project;
import com.example.weup.entity.User;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.ProjectRepository;
import com.example.weup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

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

    @Transactional
    public Map<String, Object> createTestProjects(Long userId) {

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

            Project project = new Project();
            project.setName("이름 - " + user.getName());

            Project savedProject = projectRepository.save(project);

            Member member = new Member();
            member.setUser(user);
            member.setProject(savedProject);
            member.setRole("LEADER");

            Member savedMember = memberRepository.save(member);

            Map<String, Object> result = new HashMap<>();
            result.put("projectId", savedProject.getId());
            result.put("projectName", savedProject.getName());
            result.put("userId", user.getUserId());
            result.put("userName", user.getName());
            result.put("memberRole", savedMember.getRole());
            
            return result;
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }
    }
} 