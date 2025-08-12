package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.ProjectCreateRequestDTO;
import com.example.weup.dto.request.ProjectDescriptionUpdateRequestDTO;
import com.example.weup.dto.request.ProjectEditRequestDTO;
import com.example.weup.dto.response.DetailProjectResponseDTO;
import com.example.weup.dto.response.ListUpProjectResponseDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.*;
import com.example.weup.validate.MemberValidator;
import com.example.weup.validate.ProjectValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    private final BoardRepository boardRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final TodoMemberRepository todoMemberRepository;
    private final StringRedisTemplate redisTemplate;
    private final FileRepository fileRepository;
    private final RoleRepository roleRepository;
    private final TodoRepository todoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${project.default-image}")
    private String defaultProjectImage;

    private String lockKey(Long projectId) {
        return "lock:project:" + projectId;
    }

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

        projectRepository.save(newProject);
        newProject.editProjectRoomName(String.valueOf(newProject.getProjectId()));

        log.info("create project -> db save success : project id - {}", newProject.getProjectId());
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
                .memberId(member.getMemberId())
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

//    @Transactional
//    public void editProjectDescription(Long userId, Long projectId, String description) {
//
//        Project project = projectValidator.validateActiveProject(projectId);
//        memberValidator.validateActiveMemberInProject(userId, projectId);
//
//        project.editProjectDescription(description);
//
//        projectRepository.save(project);
//        log.info("edit project description -> db save success : project id - {}", project.getProjectId());
//    }

    @Transactional
    public void deleteProject(Long userId, Long projectId) {

        Project project = projectValidator.validateAccessToGetProjectDetail(projectId);
        memberValidator.validateActiveMemberInProject(userId, projectId);
        memberValidator.isLeader(userId, project);

        project.editProjectDeletedTime(LocalDateTime.now());

        projectRepository.save(project);
        log.info("delete project -> db save success : project id - {}", project.getProjectId());
    }

    @Transactional
    public void restoreProject(Long userId, Long projectId) {

        Project project = projectValidator.validateRestoreProject(projectId);
        memberValidator.validateActiveMemberInProject(userId, projectId);

        project.editProjectDeletedTime(null);

        projectRepository.save(project);
        log.info("restore project -> db save success : project id - {}", project.getProjectId());
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredProjects() {

        LocalDateTime limitTime = LocalDateTime.now().minusDays(30);
        List<Project> projectToDelete = projectRepository.findByProjectDeletedTimeBefore(limitTime);
        log.info("delete project test -> db read data size - {}", projectToDelete.size());

        for (Project project : projectToDelete) {
            log.info("delete project -> db read success : project id - {}", project.getProjectId());

            List<Board> boardsToDelete = boardRepository.findByProject(project);
            for (Board board : boardsToDelete) {
                fileRepository.deleteByBoard(board);
                log.info("delete project -> File db data deleted");
            }
            boardRepository.deleteByProject(project);
            log.info("delete project -> Board db data deleted");

            // 채팅방, 채팅 메시지
            List<ChatRoom> chatRoomsToDelete = chatRoomRepository.findByProject(project);
            for (ChatRoom chatRoom : chatRoomsToDelete) {
                redisTemplate.delete("chat:room:"+chatRoom.getChatRoomId());
                log.info("delete project -> redis db chatting data deleted");
                chatMessageRepository.deleteByChatRoom(chatRoom);
            }
            log.info("delete project -> Chat Message db data deleted");
            chatRoomRepository.deleteAll(chatRoomsToDelete);
            log.info("delete project -> Chat Room db data deleted");

            // 멤버 역할, 역할, 투두 담당자, 투두, 멤버
            List<Member> membersToDelete = memberRepository.findByProject(project);
            for (Member member : membersToDelete) {
                log.info("delete project -> db read success : project id: {}, member id : {}", member.getProject().getProjectId(), member.getMemberId());

                memberRoleRepository.deleteByMember(member);
                log.info("delete project -> Member Role db data deleted");

                todoMemberRepository.deleteByMember(member);
                log.info("delete project -> Todo Member db data deleted");
            }

            roleRepository.deleteByProject(project);
            log.info("delete project -> Role db data deleted");

            todoRepository.deleteByProject(project);
            log.info("delete project -> Todo db data deleted");

            memberRepository.deleteAll(membersToDelete);
            log.info("delete project -> Member db data deleted");
        }

        // 프로젝트
        projectRepository.deleteAll(projectToDelete);
        log.info("delete project -> Project db data deleted");
    }

    public void startEditProjectDescription(Long userId, Long projectId) {
        Member member = memberValidator.validateActiveMemberInProject(userId, projectId);

        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey(projectId), "locked", 3, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(success)) {
            throw new GeneralException(ErrorInfo.IS_EDITING_NOW);
        }

        messagingTemplate.convertAndSend(
                "/topic/project/" + projectId,
                Map.of("type", "EDIT_LOCK",
                        "lockedBy", member.getUser().getName())
        );
    }

    public void broadcastProjectDescriptionUpdate(ProjectDescriptionUpdateRequestDTO projectDescriptionUpdateRequestDTO, Long projectId) {
        Member member = memberValidator.validateActiveMemberInProject(projectDescriptionUpdateRequestDTO.getUserId(), projectId);

        messagingTemplate.convertAndSend(
                "/topic/project/" + projectId,
                Map.of("type", "EDIT_UPDATE",
                        "description", projectDescriptionUpdateRequestDTO.getDescription(),
                        "editedBy", member.getUser().getName())
        );
    }

    @Transactional
    public void editProjectDescription(Long userId, Long projectId, String description) {
        Project project = projectValidator.validateActiveProject(projectId);
        memberValidator.validateActiveMemberInProject(userId, projectId);

        project.editProjectDescription(description);
        projectRepository.save(project);

        redisTemplate.delete(lockKey(projectId));

        messagingTemplate.convertAndSend(
                "/topic/project/" + projectId,
                Map.of("type", "EDIT_UNLOCK")
        );
    }
}