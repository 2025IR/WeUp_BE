package com.example.weup.service;

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

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if ("ROLE_ADMIN".equals(user.getRole())) {
            return true;
        }

        // 2. 프로젝트 조회
        try {
            Project project = projectRepository.findById(projectId)
                    .orElse(null);

            if (project == null) {
                return false;
            }

            // 3. 권한 확인 (멤버에서)
            try {
                boolean isMember = memberRepository.existsByUserAndProject(user, project);
                return isMember;
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
            // 1. 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // 2. 프로젝트 생성
            Project project = new Project();
            project.setName("이름 - " + user.getName());

            Project savedProject = projectRepository.save(project);

            // 3. 멤버 생성
            Member member = new Member();
            member.setUser(user);
            member.setProject(savedProject);
            member.setRole("LEADER");

            Member savedMember = memberRepository.save(member);

            // 4. 결과 반환
            Map<String, Object> result = new HashMap<>();
            result.put("projectId", savedProject.getId());
            result.put("projectName", savedProject.getName());
            result.put("userId", user.getUserId());
            result.put("userName", user.getName());
            result.put("memberRole", savedMember.getRole());
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("테스트 프로젝트 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }
} 