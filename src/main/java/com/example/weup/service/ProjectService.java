package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.ProjectCreateRequestDTO;
import com.example.weup.dto.request.ProjectEditRequestDTO;
import com.example.weup.dto.response.DetailProjectResponseDTO;
import com.example.weup.dto.response.ListUpProjectResponseDTO;
import com.example.weup.entity.ChatRoom;
import com.example.weup.entity.Member;
import com.example.weup.entity.Project;
import com.example.weup.repository.ChatRoomRepository;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.ProjectRepository;
import com.example.weup.validate.MemberValidator;
import com.example.weup.validate.ProjectValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    private final MemberRepository memberRepository;

    private final S3Service s3Service;

    private final ChatRoomRepository chatRoomRepository;

    private final MemberValidator memberValidator;
    private final ProjectValidator projectValidator;

    @Value("${project.default-image}")
    private String defaultProjectImage;

    @Transactional
    public Project createProject(ProjectCreateRequestDTO projectCreateRequestDTO) throws IOException {

        String storedFileName;
        MultipartFile image = projectCreateRequestDTO.getProjectImage();

        if (image != null && !image.isEmpty()) {
            storedFileName = s3Service.uploadSingleFile(image).getStoredFileName();
        } else {
            storedFileName = defaultProjectImage;
        }

        Project newProject = Project.builder()
                .projectName(projectCreateRequestDTO.getProjectName())
                .projectImage(storedFileName)
                .build();

        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomId(newProject.getProjectId())
                .project(newProject)
                .build();

        projectRepository.save(newProject);
        newProject.editProjectRoomName(String.valueOf(newProject.getProjectId()));
        log.info("create project -> db save success : project id - {}", newProject.getProjectId());

        chatRoomRepository.save(chatRoom);
        log.info("create chat room -> db save success : chat room id - {}", chatRoom.getChatRoomId());

        return newProject;
    }

    public List<ListUpProjectResponseDTO> getProjectList(Long userId) {

        List<Member> activeMember = memberRepository.findActiveMemberByUserId(userId);
        log.info("get project list -> db read success : data size - {}", activeMember.size());

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
                            .projectImage(s3Service.getPresignedUrl(project.getProjectImage()))
                            .status(project.isStatus())
                            .projectCreatedTime(project.getProjectCreatedTime())
                            .finalTime(time)
                            .memberCount(memberCount)
                            .build();
                })
                .sorted(Comparator.comparing(ListUpProjectResponseDTO::getProjectId).reversed())
                .collect(Collectors.toList());
    }

    public DetailProjectResponseDTO getProjectDetail(Long projectId, Long userId) {

        Project project = projectValidator.validateAccessToGetProjectDetail(projectId);
        Member member = memberValidator.validateActiveMemberInProject(userId, projectId);
        log.info("get project detail -> member validate : member id - {}", member.getMemberId());

        return DetailProjectResponseDTO.builder()
                .projectName(project.getProjectName())
                .projectImage(s3Service.getPresignedUrl(project.getProjectImage()))
                .description(project.getDescription())
                .projectCreatedTime(project.getProjectCreatedTime())
                .status(project.isStatus())
                .isRevealedNumber(project.isRevealedNumber())
                .isLeader(member.isLeader())
                .build();
    }

    @Transactional
    public void editProject(Long userId, Long projectId, ProjectEditRequestDTO dto) throws IOException {

        Project project = projectValidator.validateActiveProject(projectId);
        memberValidator.validateActiveMemberInProject(userId, projectId);
        memberValidator.isLeader(userId, project);

        MultipartFile image = dto.getProjectImage();
        if (image != null && !image.isEmpty()) {
            String existingImage = project.getProjectImage();
            s3Service.deleteFile(existingImage);

            String storedFileName = s3Service.uploadSingleFile(image).getStoredFileName();
            project.editProjectImage(storedFileName);
        }

        project.editProjectInfo(dto);

        projectRepository.save(project);
        log.info("edit project information -> db save success : project id - {}", project.getProjectId());
    }

    @Transactional
    public void editProjectDescription(Long userId, Long projectId, String description) {

        Project project = projectValidator.validateActiveProject(projectId);
        memberValidator.validateActiveMemberInProject(userId, projectId);

        project.editProjectDescription(description);

        projectRepository.save(project);
        log.info("edit project description -> db save success : project id - {}", project.getProjectId());
    }

}