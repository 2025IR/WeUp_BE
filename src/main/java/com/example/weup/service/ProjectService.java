package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.CreateProjectDTO;
import com.example.weup.dto.response.DetailProjectResponseDTO;
import com.example.weup.dto.response.ListUpProjectResponseDTO;
import com.example.weup.entity.Member;
import com.example.weup.entity.Project;
import com.example.weup.entity.User;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.ProjectRepository;
import com.example.weup.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional
    public Project createProject(CreateProjectDTO createProjectDto) {

        Project newProject = Project.builder()
                .projectName(createProjectDto.getProjectName())
                .projectImage(createProjectDto.getProjectImage())
                .build();

        projectRepository.save(newProject);

        return newProject;
    }

    @Transactional
    public List<ListUpProjectResponseDTO> listUpProject(Long userId) {

        List<Member> activeMember = memberRepository.findActiveMemberByUserId(userId);

        return activeMember.stream().map(member -> {

            Project project = member.getProject();

            int memberCount = (int) project.getMembers().stream()
                    .filter(m -> !m.isMemberDeleted())
                    .count();

            LocalDateTime time = project.getProjectEndedTime() != null
                    ? project.getProjectEndedTime().atStartOfDay()
                    : member.getLastAccessTime();

            return ListUpProjectResponseDTO.builder()
                    .projectId(project.getProjectId())
                    .projectName(project.getProjectName())
                    .projectImage(project.getProjectImage())
                    .projectCreatedTime(project.getProjectCreatedTime())
                    .finalTime(time)
                    .memberCount(memberCount)
                    .build();
            })
                .collect(Collectors.toList());
    }

    @Transactional
    public DetailProjectResponseDTO detailProject(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        return DetailProjectResponseDTO.builder()
                .projectName(project.getProjectName())
                .projectImage(project.getProjectImage())
                .description(project.getDescription())
                .build();
    }

    @Transactional
    public void changeProjectStatus(Long userId, Long projectId, Boolean status) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        if(!isLeader(userId, project)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        project.setStatus(status);

        projectRepository.save(project);
    }

    @Transactional
    public void editProject(Long userId, Long projectId, CreateProjectDTO createProjectDto) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        if(!isLeader(userId, project)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        project.setProjectName(createProjectDto.getProjectName());
        project.setProjectImage(createProjectDto.getProjectImage());
        project.setDescription(project.getDescription());

        projectRepository.save(project);
    }

    @Transactional
    public void editProjectDescription(Long projectId, String description) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        project.setDescription(description);

        projectRepository.save(project);
    }

    private boolean isLeader(Long userId, Project project) {

        User user = userRepository.findById(userId)
                        .orElseThrow(() -> new GeneralException(ErrorInfo.USER_NOT_FOUND));

        Optional<Member> member = memberRepository.findByUserAndProject(user, project);

        return member.isPresent() && member.get().isLeader();
    }

} 