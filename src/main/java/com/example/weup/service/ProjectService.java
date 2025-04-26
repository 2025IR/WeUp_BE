package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.CreateProjectDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.DetailProjectResponseDTO;
import com.example.weup.dto.response.ListUpProjectResponseDTO;
import com.example.weup.entity.Project;
import com.example.weup.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public Long createProject(CreateProjectDTO createProjectDto) {

        log.debug("createProjectDto: {}", createProjectDto);

        Project newProject = Project.builder()
                .projectName(createProjectDto.getProjectName())
                .projectImage(createProjectDto.getProjectImage())
                .build();

        log.debug("new project description: {}", newProject.getDescription());

        return projectRepository.save(newProject).getProjectId();
    }

//    @Transactional
//    public List<ListUpProjectResponseDTO> listUpProject(Long userId) {
//
//        ListUpProjectResponseDTO list;
//    }

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
    public void editProject(Long projectId, CreateProjectDTO createProjectDto) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));



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

} 