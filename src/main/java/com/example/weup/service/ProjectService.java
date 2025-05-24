package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.ProjectCreateRequestDTO;
import com.example.weup.dto.request.ProjectEditRequestDTO;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
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

    private final S3Service s3Service;

    @Transactional
    public Project createProject(@ModelAttribute ProjectCreateRequestDTO projectCreateRequestDTO) throws IOException {
        String storedFileName = null;
        MultipartFile image = projectCreateRequestDTO.getFile();


        if (image != null && !image.isEmpty()) {
            storedFileName = s3Service.uploadSingleFile(image).getStoredFileName();
        } else {
            storedFileName = "086d1ece-d1dd-424b-97ae-892075355026-smiley1.png";
        }

        Project newProject = Project.builder()
                .projectName(projectCreateRequestDTO.getProjectName())
                .projectImage(storedFileName)
                .build();

        return projectRepository.save(newProject);
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
                    .projectImage(s3Service.getPresignedUrl(project.getProjectImage()))
                    .status(project.isStatus())
                    .projectCreatedTime(project.getProjectCreatedTime())
                    .finalTime(time)
                    .memberCount(memberCount)
                    .build();
            })
                .sorted(Comparator.comparing(ListUpProjectResponseDTO::getProjectCreatedTime).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public DetailProjectResponseDTO detailProject(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        return DetailProjectResponseDTO.builder()
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
    public void editProject(Long userId, Long projectId, ProjectEditRequestDTO dto) throws IOException {

        log.debug("DTO 확인 - image : " + dto.getProjectImage());
        log.debug("DTO 확인 - isRevealed : " + dto.isRevealedNumber());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        if (!isLeader(userId, project)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        MultipartFile image = dto.getProjectImage();
        if (image != null && !image.isEmpty()) {
            String existingImage = project.getProjectImage();
            if (existingImage != null && !existingImage.isEmpty()) {
                s3Service.deleteFile(existingImage);
            }

            String storedFileName = s3Service.uploadSingleFile(image).getStoredFileName();
            log.debug("DTO 확인 - image2 : " + dto.getProjectImage());
            project.setProjectImage(storedFileName);
        }

        projectRepository.save(project);
        log.debug("service - setProjectImage : " + project.getProjectImage());

        project.setProjectName(dto.getProjectName());
        log.debug("service - setProjectName : " + project.getProjectName());

        project.setStatus(dto.isStatus());
        log.debug("service - setStatus : " + project.isStatus());

        project.setRevealedNumber(dto.isRevealedNumber());
        log.debug("service - setRevealedNumber : " + project.isRevealedNumber());
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